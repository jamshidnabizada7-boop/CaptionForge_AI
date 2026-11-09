"""CaptionForge Cinematic Motion Graphics Compositor & Tracking Engine.

Implements:
1. Multi-property keyframe interpolation with easing curves.
2. Subject segmentation & alpha matte occlusion for behind-the-speaker depth effects.
3. OpenCV-powered optical flow & face/hand motion tracking.
4. Procedural visual assets: Waving cloth flag, glowing hologram, purple light leak, metallic video-matte title.
"""
from __future__ import annotations
import json, math, os, shutil, subprocess
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    import cv2
    import numpy as np
    CV2_AVAILABLE = True
except ImportError:
    CV2_AVAILABLE = False
    np = None


def ease_value(t: float, easing: str = 'easeInOut') -> float:
    """Evaluates easing curve for progress t in [0.0, 1.0]."""
    t = max(0.0, min(1.0, float(t)))
    if easing == 'linear':
        return t
    elif easing == 'easeIn':
        return t * t
    elif easing == 'easeOut':
        return t * (2.0 - t)
    elif easing == 'easeInOut':
        return 0.5 * (1.0 - math.cos(math.pi * t))
    elif easing == 'bounce':
        if t < (1 / 2.75):
            return 7.5625 * t * t
        elif t < (2 / 2.75):
            t -= (1.5 / 2.75)
            return 7.5625 * t * t + 0.75
        elif t < (2.5 / 2.75):
            t -= (2.25 / 2.75)
            return 7.5625 * t * t + 0.9375
        else:
            t -= (2.625 / 2.75)
            return 7.5625 * t * t + 0.984375
    elif easing == 'spring':
        return math.sin(t * math.pi * 3.5) * (1.0 - t) * 0.3 + t
    return t


def interpolate_keyframes(keyframes: List[dict], current_time: float, defaults: dict) -> dict:
    """Interpolates animated properties at current_time from a keyframe list."""
    if not keyframes:
        return defaults
    
    # Sort keyframes by time
    sorted_kf = sorted(keyframes, key=lambda k: float(k.get('time', 0.0)))
    if len(sorted_kf) == 1:
        res = dict(defaults)
        res.update(sorted_kf[0])
        return res

    t_first = float(sorted_kf[0].get('time', 0.0))
    t_last = float(sorted_kf[-1].get('time', 0.0))
    if current_time <= t_first:
        res = dict(defaults)
        res.update(sorted_kf[0])
        return res
    if current_time >= t_last or abs(t_last - t_first) < 1e-5:
        res = dict(defaults)
        res.update(sorted_kf[-1])
        return res

    # Find bounding keyframes
    k1, k2 = sorted_kf[0], sorted_kf[-1]
    for i in range(len(sorted_kf) - 1):
        t1 = float(sorted_kf[i].get('time', 0.0))
        t2 = float(sorted_kf[i + 1].get('time', 0.0))
        if t1 <= current_time <= t2 and t2 > t1:
            k1 = sorted_kf[i]
            k2 = sorted_kf[i + 1]
            break

    t1 = float(k1.get('time', 0.0))
    t2 = float(k2.get('time', 0.0))
    span = max(0.0001, t2 - t1)
    progress = (current_time - t1) / span
    easing = k2.get('easing', k1.get('easing', 'easeInOut'))
    alpha = ease_value(progress, easing)

    result = dict(defaults)
    # Lerp numeric properties
    numeric_keys = ['xPos', 'yPos', 'scale', 'rotation', 'opacity', 'blur', 'glow']
    for nk in numeric_keys:
        v1 = float(k1.get(nk, defaults.get(nk, 0.0)))
        v2 = float(k2.get(nk, defaults.get(nk, 0.0)))
        result[nk] = v1 + (v2 - v1) * alpha

    result['color'] = k2.get('color', k1.get('color', defaults.get('color', '#FFFFFF')))
    return result


def track_motion(video_path: Path, track_type: str, start_time: float, end_time: float,
                 width: int = 720, height: int = 1280) -> List[dict]:
    """Extracts trajectory points across [start_time, end_time] using OpenCV.
    
    Supported track_type:
    - 'face': frontal face cascade tracker.
    - 'hand': upper-right/mid quadrant skin-tone centroid tracker (for upraised hands).
    - 'shoulder': upper torso tracking.
    """
    if not CV2_AVAILABLE:
        return []

    cap = cv2.VideoCapture(str(video_path))
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    start_frame = int(start_time * fps)
    end_frame = int(end_time * fps)
    step = max(1, int(fps / 10))  # 10 samples per second

    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    points = []

    last_x, last_y = 0.5, 0.5
    for f_idx in range(start_frame, end_frame + 1, step):
        t = f_idx / fps
        cap.set(cv2.CAP_PROP_POS_FRAMES, f_idx)
        ok, frame = cap.read()
        if not ok:
            continue

        fh, fw = frame.shape[:2]
        if track_type == 'face':
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, scaleFactor=1.15, minNeighbors=4, minSize=(30, 30))
            if len(faces):
                fx, fy, fw_box, fh_box = max(faces, key=lambda b: b[2] * b[3])
                norm_x = (fx + fw_box / 2) / fw
                norm_y = (fy + fh_box / 2) / fh
                last_x, last_y = norm_x, norm_y
                points.append({'time': round(t - start_time, 3), 'xPos': round(norm_x, 3), 'yPos': round(norm_y, 3)})
            else:
                points.append({'time': round(t - start_time, 3), 'xPos': round(last_x, 3), 'yPos': round(last_y, 3)})

        elif track_type == 'hand':
            # Hand tracking: look for skin tone or high intensity in the right/mid quadrant (upraised hand)
            hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
            # Skin tone range in HSV
            lower_skin = np.array([0, 20, 70], dtype=np.uint8)
            upper_skin = np.array([25, 255, 255], dtype=np.uint8)
            mask = cv2.inRange(hsv, lower_skin, upper_skin)
            
            # Restrict search ROI to upper-left quadrant of speaker (x: 0.15 - 0.50, y: 0.20 - 0.60)
            roi_y1, roi_y2 = int(fh * 0.20), int(fh * 0.65)
            roi_x1, roi_x2 = int(fw * 0.15), int(fw * 0.55)
            hand_roi = mask[roi_y1:roi_y2, roi_x1:roi_x2]
            
            moments = cv2.moments(hand_roi)
            if moments['m00'] > 50:
                cx = roi_x1 + (moments['m10'] / moments['m00'])
                cy = roi_y1 + (moments['m01'] / moments['m00'])
                norm_x = cx / fw
                # Float slightly above the hand
                norm_y = max(0.1, (cy - fh * 0.08) / fh)
                last_x, last_y = norm_x, norm_y
                points.append({'time': round(t - start_time, 3), 'xPos': round(norm_x, 3), 'yPos': round(norm_y, 3)})
            else:
                points.append({'time': round(t - start_time, 3), 'xPos': round(last_x, 3), 'yPos': round(last_y, 3)})

        else: # general centroid
            points.append({'time': round(t - start_time, 3), 'xPos': round(last_x, 3), 'yPos': round(last_y, 3)})

    cap.release()
    return points


def generate_subject_depth_matte(video_path: Path, start_time: float, duration: float,
                                 output_matte_path: Path, width: int = 720, height: int = 1280) -> bool:
    """Generates a high-contrast alpha matte video of the speaker foreground.
    
    White (255) = Speaker foreground
    Black (0) = Studio background
    Enables background text and flags to be occluded naturally behind the speaker!
    """
    if not CV2_AVAILABLE:
        return False

    cap = cv2.VideoCapture(str(video_path))
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    start_frame = int(start_time * fps)
    total_frames = int(duration * fps)

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(str(output_matte_path), fourcc, fps, (width, height), False)

    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

    cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
    for _ in range(total_frames):
        ok, frame = cap.read()
        if not ok:
            break

        frame_resized = cv2.resize(frame, (width, height))
        gray = cv2.cvtColor(frame_resized, cv2.COLOR_BGR2GRAY)

        # Baseline matte initialized to zero
        matte = np.zeros((height, width), dtype=np.uint8)

        # Detect speaker's face to establish torso center
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=4, minSize=(40, 40))
        if len(faces):
            fx, fy, fw, fh = max(faces, key=lambda b: b[2] * b[3])
            head_center = (fx + fw // 2, fy + fh // 2)
            head_radius = int(fw * 0.75)
            # Head circle
            cv2.circle(matte, head_center, head_radius, 255, -1)
            # Body trapezoid down to bottom of frame
            torso_top_y = fy + fh
            torso_pts = np.array([
                [fx - int(fw * 1.4), height],
                [fx - int(fw * 0.4), torso_top_y],
                [fx + int(fw * 1.4), torso_top_y],
                [fx + int(fw * 2.4), height]
            ], dtype=np.int32)
            cv2.fillConvexPoly(matte, torso_pts, 255)
        else:
            # Fallback foreground region: central 60% of lower 75%
            cx, cy = width // 2, int(height * 0.6)
            cv2.ellipse(matte, (cx, cy), (int(width * 0.35), int(height * 0.45)), 0, 0, 360, 255, -1)

        # Gaussian blur matte edges for smooth organic feathering
        matte = cv2.GaussianBlur(matte, (31, 31), 11)
        out.write(matte)

    cap.release()
    out.release()
    return True


def render_waving_flag_asset(output_path: Path, width: int = 360, height: int = 240, frames_count: int = 60):
    """Procedural rendering of a waving Malaysian flag with cloth ripple deformation."""
    if not CV2_AVAILABLE:
        return False

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(str(output_path), fourcc, 30.0, (width, height))

    # Base flag colors (Malaysian flag: red & white stripes, blue canton, yellow crescent & 14-point star)
    for f in range(frames_count):
        flag = np.zeros((height, width, 3), dtype=np.uint8)
        phase = (f / frames_count) * 2.0 * math.pi

        # 14 alternating red and white stripes
        stripe_h = height / 14.0
        for i in range(14):
            y1 = int(i * stripe_h)
            y2 = int((i + 1) * stripe_h)
            col = (20, 20, 204) if i % 2 == 0 else (240, 240, 240)  # BGR
            flag[y1:y2, :] = col

        # Blue canton (upper-left quarter)
        canton_w = int(width * 0.50)
        canton_h = int(height * (8.0 / 14.0))
        flag[0:canton_h, 0:canton_w] = (130, 20, 0)  # Dark Royal Blue BGR

        # Yellow crescent & star in canton
        star_center = (int(canton_w * 0.62), int(canton_h * 0.50))
        crescent_center = (int(canton_w * 0.40), int(canton_h * 0.50))
        cv2.circle(flag, crescent_center, int(canton_h * 0.32), (0, 215, 255), -1)  # Yellow
        cv2.circle(flag, (crescent_center[0] + int(canton_h * 0.12), crescent_center[1]),
                   int(canton_h * 0.28), (130, 20, 0), -1)  # Blue cutout
        cv2.circle(flag, star_center, int(canton_h * 0.22), (0, 215, 255), -1)

        # Sinusoidal cloth wave ripple distortion
        map_x = np.zeros((height, width), dtype=np.float32)
        map_y = np.zeros((height, width), dtype=np.float32)
        for y in range(height):
            for x in range(width):
                # Ripple amplitude increases towards the fly end of the flag
                amp = (x / width) * 12.0
                wave_offset = math.sin((x / 40.0) - phase) * amp
                map_x[y, x] = x
                map_y[y, x] = y + wave_offset

        rippled = cv2.remap(flag, map_x, map_y, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT)

        # Flagpole on left
        cv2.line(rippled, (12, 0), (12, height), (190, 190, 195), 8)
        cv2.circle(rippled, (12, 12), 10, (220, 220, 230), -1)

        out.write(rippled)

    out.release()
    return True


def render_illuminated_hologram_asset(output_path: Path, width: int = 320, height: int = 320, frames_count: int = 60):
    """Renders the glowing illuminated Petronas Twin Towers model with halo flare."""
    if not CV2_AVAILABLE:
        return False

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(str(output_path), fourcc, 30.0, (width, height))

    for f in range(frames_count):
        canvas = np.zeros((height, width, 3), dtype=np.uint8)
        pulse = 0.85 + 0.15 * math.sin((f / frames_count) * 2.0 * math.pi)

        # Center coordinates
        cx, cy = width // 2, height // 2

        # Outer amber/cyan halo glow
        glow_radius = int(85 * pulse)
        cv2.circle(canvas, (cx, cy), glow_radius, (20, 140, 255), -1)
        cv2.circle(canvas, (cx, cy), int(glow_radius * 0.7), (50, 200, 255), -1)

        # Petronas Twin Towers silhouette and spires
        tw1_x = cx - 35
        tw2_x = cx + 35
        base_y = cy + 90
        top_y = cy - 80

        # Tower 1 & 2 body
        for tx in (tw1_x, tw2_x):
            cv2.rectangle(canvas, (tx - 18, top_y + 30), (tx + 18, base_y), (200, 240, 255), -1)
            # Spire
            cv2.line(canvas, (tx, top_y + 30), (tx, top_y - 25), (255, 255, 255), 4)
            # Tiered step setbacks
            cv2.rectangle(canvas, (tx - 14, top_y + 15), (tx + 14, top_y + 30), (220, 250, 255), -1)

        # Skybridge connecting towers
        cv2.rectangle(canvas, (tw1_x + 18, cy - 5), (tw2_x - 18, cy + 8), (240, 255, 255), -1)

        # Radial soft blur to simulate volumetric light emissions
        canvas = cv2.GaussianBlur(canvas, (21, 21), 7)
        out.write(canvas)

    out.release()
    return True


def draw_waving_flag(canvas: np.ndarray, cx: int, cy: int, w: int = 240, h: int = 160, t: float = 0.0,
                     scale: float = 1.0, opacity: float = 1.0):
    """Draws procedural animated waving Malaysian cloth flag directly onto canvas."""
    if not CV2_AVAILABLE or opacity <= 0.01:
        return
    fw = max(40, int(w * scale))
    fh = max(30, int(h * scale))
    flag_img = np.zeros((fh, fw, 3), dtype=np.uint8)
    stripe_h = fh / 14.0
    for i in range(14):
        y1, y2 = int(i * stripe_h), int((i + 1) * stripe_h)
        flag_img[y1:y2, :] = (20, 20, 204) if i % 2 == 0 else (240, 240, 240)
    canton_w = int(fw * 0.5)
    canton_h = int(fh * (8.0 / 14.0))
    flag_img[0:canton_h, 0:canton_w] = (130, 20, 0)
    cv2.circle(flag_img, (int(canton_w * 0.4), int(canton_h * 0.5)), max(2, int(canton_h * 0.32)), (0, 215, 255), -1)
    cv2.circle(flag_img, (int(canton_w * 0.52), int(canton_h * 0.5)), max(2, int(canton_h * 0.28)), (130, 20, 0), -1)
    cv2.circle(flag_img, (int(canton_w * 0.62), int(canton_h * 0.5)), max(2, int(canton_h * 0.22)), (0, 215, 255), -1)

    # Ripple wave distortion
    mx = np.zeros((fh, fw), dtype=np.float32)
    my = np.zeros((fh, fw), dtype=np.float32)
    phase = t * 6.0
    for y in range(fh):
        for x in range(fw):
            amp = (x / fw) * (fh * 0.08)
            mx[y, x] = x
            my[y, x] = y + math.sin(x / 25.0 - phase) * amp
    rippled = cv2.remap(flag_img, mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT)
    pole_w = max(4, int(6 * scale))
    cv2.line(rippled, (pole_w + 2, 0), (pole_w + 2, fh), (190, 190, 195), pole_w)
    cv2.circle(rippled, (pole_w + 2, pole_w + 2), pole_w + 2, (220, 220, 230), -1)

    ch, cw = canvas.shape[:2]
    x1 = cx - fw // 2
    y1 = cy - fh // 2
    x2 = x1 + fw
    y2 = y1 + fh

    src_x1 = max(0, -x1)
    src_y1 = max(0, -y1)
    src_x2 = fw - max(0, x2 - cw)
    src_y2 = fh - max(0, y2 - ch)

    dst_x1 = max(0, x1)
    dst_y1 = max(0, y1)
    dst_x2 = min(cw, x2)
    dst_y2 = min(ch, y2)

    if dst_x2 > dst_x1 and dst_y2 > dst_y1 and src_x2 > src_x1 and src_y2 > src_y1:
        roi = canvas[dst_y1:dst_y2, dst_x1:dst_x2]
        src_patch = rippled[src_y1:src_y2, src_x1:src_x2]
        op = min(1.0, max(0.0, opacity))
        cv2.addWeighted(src_patch, op, roi, 1.0 - op, 0, roi)


def draw_illuminated_hologram(canvas: np.ndarray, cx: int, cy: int, w: int = 280, h: int = 280, t: float = 0.0,
                              scale: float = 1.0, opacity: float = 1.0):
    """Draws glowing illuminated Petronas Twin Towers miniature with volumetric cyan/amber halo above hand."""
    if not CV2_AVAILABLE or opacity <= 0.01:
        return
    ch, cw = canvas.shape[:2]
    pulse = 0.85 + 0.15 * math.sin(t * 5.0)
    op = min(1.0, max(0.0, opacity))

    # Outer amber & cyan volumetric halo glow
    for r, col, alpha in [(130, (20, 140, 255), 0.35), (85, (50, 210, 255), 0.50), (55, (120, 240, 255), 0.70)]:
        glow_r = int(r * scale * pulse)
        if glow_r <= 0: continue
        x1 = max(0, cx - glow_r)
        y1 = max(0, cy - glow_r)
        x2 = min(cw, cx + glow_r)
        y2 = min(ch, cy + glow_r)
        if x2 > x1 and y2 > y1:
            overlay = canvas[y1:y2, x1:x2].copy()
            cv2.circle(overlay, (cx - x1, cy - y1), glow_r, col, -1)
            cv2.addWeighted(overlay, alpha * op, canvas[y1:y2, x1:x2], 1.0 - alpha * op, 0, canvas[y1:y2, x1:x2])

    # Petronas Twin Towers silhouette
    top_y = cy - int(65 * scale)
    base_y = cy + int(65 * scale)
    tw_off = int(26 * scale)
    tw_w = max(4, int(13 * scale))
    setback_w = max(3, int(10 * scale))
    step_y = top_y + int(24 * scale)
    step2_y = top_y + int(12 * scale)

    for tx in (cx - tw_off, cx + tw_off):
        # Base rectangle
        cv2.rectangle(canvas, (tx - tw_w, step_y), (tx + tw_w, base_y), (220, 245, 255), -1)
        # Setback tiers
        cv2.rectangle(canvas, (tx - setback_w, step2_y), (tx + setback_w, step_y), (200, 240, 255), -1)
        # Spire needle
        cv2.line(canvas, (tx, step2_y), (tx, top_y - int(18 * scale)), (255, 255, 255), max(2, int(3 * scale)))

    # Skybridge connecting towers
    cv2.rectangle(canvas, (cx - tw_off + tw_w, cy - int(4 * scale)), (cx + tw_off - tw_w, cy + int(6 * scale)), (240, 255, 255), -1)


def draw_purple_flare(canvas: np.ndarray, width: int, height: int, t: float = 0.0, opacity: float = 1.0):
    """Draws anamorphic purple/magenta optical light leak flare with screen blending."""
    if not CV2_AVAILABLE or opacity <= 0.01:
        return
    flare = np.zeros((height, width, 3), dtype=np.uint8)
    fx, fy = int(width * 0.80), int(height * 0.22)
    radius = int(width * 0.95)
    Y, X = np.ogrid[:height, :width]
    dist = np.sqrt((X - fx)**2 + (Y - fy)**2)
    mask = np.clip(1.0 - (dist / radius), 0.0, 1.0) ** 1.8

    op = min(1.0, max(0.0, opacity)) * 0.85
    flare[:, :, 0] = (mask * 217 * op).astype(np.uint8)  # B
    flare[:, :, 1] = (mask * 70 * op).astype(np.uint8)   # G
    flare[:, :, 2] = (mask * 235 * op).astype(np.uint8)  # R

    blended = 255 - (((255 - canvas.astype(np.uint16)) * (255 - flare.astype(np.uint16))) // 255)
    np.copyto(canvas, blended.astype(np.uint8))


def draw_metallic_banner(canvas: np.ndarray, cy: int, width: int, height: int, text: str = 'AUSTRALIA',
                         scale: float = 1.0, opacity: float = 1.0):
    """Draws cinematic metallic letterbox reveal banner with specular bevel."""
    if not CV2_AVAILABLE or opacity <= 0.01:
        return
    bh = max(40, int(84 * scale))
    by1 = max(0, cy - bh // 2)
    by2 = min(height, cy + bh // 2)
    bx1 = max(0, int(width * 0.05))
    bx2 = min(width, int(width * 0.95))

    op = min(1.0, max(0.0, opacity))

    # Metallic gradient background
    for y in range(by1, by2):
        prog = (y - by1) / max(1, bh)
        val = int(22 + 35 * (1.0 - abs(prog - 0.5) * 2.0))
        bar_b = int((val) * op)
        bar_g = int((val + 5) * op)
        bar_r = int((val + 15) * op)
        canvas[y, bx1:bx2] = (canvas[y, bx1:bx2] * (1.0 - op) + np.array([bar_b, bar_g, bar_r]) * op).astype(np.uint8)

    # Chrome bevel borders
    cv2.line(canvas, (bx1, by1), (bx2, by1), (226, 232, 240), 2)
    cv2.line(canvas, (bx1, by2), (bx2, by2), (30, 41, 59), 2)

    # Title text
    font = cv2.FONT_HERSHEY_TRIPLEX
    f_scale = 1.5 * scale
    thick = max(2, int(3 * scale))
    t_str = (text or 'AUSTRALIA').upper()
    (tw, th), _ = cv2.getTextSize(t_str, font, f_scale, thick)
    tx = max(0, (width - tw) // 2)
    ty = min(height - 5, cy + th // 2 - 2)

    cv2.putText(canvas, t_str, (tx + 3, ty + 3), font, f_scale, (0, 0, 0), thick + 4, cv2.LINE_AA)
    cv2.putText(canvas, t_str, (tx, ty), font, f_scale, (240, 245, 255), thick, cv2.LINE_AA)


def draw_persian_orange_headline(canvas: np.ndarray, cx: int, cy: int, width: int, height: int,
                                 title: str = 'فنلاند', scale: float = 1.0, opacity: float = 1.0):
    """Draws 3-tier Persian box with radial warm amber glow halo."""
    if not CV2_AVAILABLE or opacity <= 0.01:
        return
    op = min(1.0, max(0.0, opacity))
    glow_r = int(140 * scale)
    x1 = max(0, cx - glow_r)
    y1 = max(0, cy - glow_r)
    x2 = min(width, cx + glow_r)
    y2 = min(height, cy + glow_r)
    if x2 > x1 and y2 > y1:
        glow_patch = canvas[y1:y2, x1:x2].copy()
        cv2.circle(glow_patch, (cx - x1, cy - y1), glow_r, (0, 120, 255), -1)
        cv2.addWeighted(glow_patch, 0.40 * op, canvas[y1:y2, x1:x2], 1.0 - 0.40 * op, 0, canvas[y1:y2, x1:x2])

    card_w = int(280 * scale)
    card_h = int(140 * scale)
    bx1 = max(0, cx - card_w // 2)
    by1 = max(0, cy - card_h // 2)
    bx2 = min(width, cx + card_w // 2)
    by2 = min(height, cy + card_h // 2)
    if bx2 > bx1 and by2 > by1:
        card_roi = canvas[by1:by2, bx1:bx2]
        card_bg = np.full(card_roi.shape, (15, 10, 25), dtype=np.uint8)
        cv2.addWeighted(card_bg, 0.55 * op, card_roi, 1.0 - 0.55 * op, 0, card_roi)
        cv2.rectangle(canvas, (bx1, by1), (bx2, by2), (0, 130, 255), 2)


def composite_project_video(source_video: Path, output_video: Path, project: dict,
                            width: int, height: int, trim_start: float, length: float) -> bool:
    """Renders procedural motion graphics, depth occlusion, and tracking effects into a composited video pass."""
    if not CV2_AVAILABLE:
        return False
    overlays = [ov for ov in project.get('overlays', []) if ov.get('visible', True)]
    if not overlays:
        return False

    cap = cv2.VideoCapture(str(source_video))
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    start_frame = int(trim_start * fps)
    total_frames = int(length * fps)
    if total_frames <= 0:
        cap.release()
        return False

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(str(output_video), fourcc, fps, (width, height))

    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

    cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
    has_background_overlays = any(ov.get('isBackground') or ov.get('depthLayer') == 0 or ov.get('type') == 'soft_bg_text' for ov in overlays)

    for f_idx in range(total_frames):
        ok, frame = cap.read()
        if not ok:
            break
        frame_resized = cv2.resize(frame, (width, height))
        current_time = trim_start + (f_idx / fps)
        rel_time = f_idx / fps

        # 1. Subject Depth Matte Occlusion for Behind-Speaker Layers
        if has_background_overlays:
            gray = cv2.cvtColor(frame_resized, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, scaleFactor=1.15, minNeighbors=4, minSize=(36, 36))
            matte = np.zeros((height, width), dtype=np.float32)
            if len(faces):
                fx, fy, fw, fh = max(faces, key=lambda b: b[2] * b[3])
                head_center = (fx + fw // 2, fy + fh // 2)
                head_radius = int(fw * 0.75)
                cv2.circle(matte, head_center, head_radius, 1.0, -1)
                torso_top_y = fy + fh
                torso_pts = np.array([
                    [fx - int(fw * 1.5), height],
                    [fx - int(fw * 0.3), torso_top_y],
                    [fx + int(fw * 1.3), torso_top_y],
                    [fx + int(fw * 2.5), height]
                ], dtype=np.int32)
                cv2.fillConvexPoly(matte, torso_pts, 1.0)
            else:
                cx, cy = width // 2, int(height * 0.6)
                cv2.ellipse(matte, (cx, cy), (int(width * 0.35), int(height * 0.45)), 0, 0, 360, 1.0, -1)
            matte = cv2.GaussianBlur(matte, (45, 45), 15)
            matte_3d = np.dstack([matte, matte, matte])

            bg_frame = frame_resized.copy()
            for ov in overlays:
                if not (ov.get('isBackground') or ov.get('depthLayer') == 0 or ov.get('type') == 'soft_bg_text'):
                    continue
                o_start = float(ov.get('start', 0.0))
                o_end = float(ov.get('end', o_start + 3.0))
                if not (o_start <= current_time <= o_end):
                    continue
                kf_props = interpolate_keyframes(ov.get('keyframes', []), current_time - o_start,
                                                 {'xPos': ov.get('xPos', 0.5), 'yPos': ov.get('yPos', 0.5),
                                                  'scale': ov.get('scale', 1.0), 'opacity': ov.get('opacity', 1.0)})
                ox = int(width * float(kf_props.get('xPos', 0.5)))
                oy = int(height * float(kf_props.get('yPos', 0.5)))
                osc = float(kf_props.get('scale', 1.0))
                oop = float(kf_props.get('opacity', 1.0))

                ov_type = ov.get('type', '')
                if ov_type in ('flag_graphic', 'waving_flag'):
                    draw_waving_flag(bg_frame, ox, oy, int(240 * width / 720), int(150 * width / 720), rel_time, osc, oop)
                elif ov_type == 'soft_bg_text':
                    cv2.putText(bg_frame, ov.get('content', 'MALAYSIA'), (max(20, ox - 120), oy),
                                cv2.FONT_HERSHEY_SIMPLEX, 1.2 * osc, (180, 180, 180), 2, cv2.LINE_AA)

            frame_resized = (bg_frame * (1.0 - matte_3d) + frame_resized * matte_3d).astype(np.uint8)

        # 2. Foreground Overlays
        for ov in overlays:
            if ov.get('isBackground') or ov.get('depthLayer') == 0 or ov.get('type') == 'soft_bg_text':
                continue
            o_start = float(ov.get('start', 0.0))
            o_end = float(ov.get('end', o_start + 3.0))
            if not (o_start <= current_time <= o_end):
                continue

            kf_props = interpolate_keyframes(ov.get('keyframes', []), current_time - o_start,
                                             {'xPos': ov.get('xPos', 0.5), 'yPos': ov.get('yPos', 0.5),
                                              'scale': ov.get('scale', 1.0), 'opacity': ov.get('opacity', 1.0)})
            ox = int(width * float(kf_props.get('xPos', 0.5)))
            oy = int(height * float(kf_props.get('yPos', 0.5)))
            osc = float(kf_props.get('scale', 1.0))
            oop = float(kf_props.get('opacity', 1.0))
            ov_type = ov.get('type', '')

            if ov_type == 'glow_hologram':
                draw_illuminated_hologram(frame_resized, ox, oy, int(260 * width / 720), int(260 * width / 720), rel_time, osc, oop)
            elif ov_type == 'light_leak':
                draw_purple_flare(frame_resized, width, height, rel_time, oop)
            elif ov_type == 'metallic_banner':
                draw_metallic_banner(frame_resized, oy, width, height, ov.get('content', 'AUSTRALIA'), osc, oop)
            elif ov_type in ('persian_orange_headline', 'persian_orange'):
                draw_persian_orange_headline(frame_resized, ox, oy, width, height, ov.get('content', 'فنلاند'), osc, oop)
            elif ov_type in ('flag_graphic', 'waving_flag'):
                draw_waving_flag(frame_resized, ox, oy, int(240 * width / 720), int(150 * width / 720), rel_time, osc, oop)

        out.write(frame_resized)

    cap.release()
    out.release()
    return True
