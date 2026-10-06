import math
import os
import random
import subprocess
import time
from PIL import Image, ImageDraw, ImageFilter

def generate_video_loop():
    print("Starting video loop generation...")
    src_img_path = "data/assets/billing_hero.png"
    if not os.path.exists(src_img_path):
        src_img_path = "data/assets/billing_hero.jpg"
    
    base = Image.open(src_img_path).convert("RGBA")
    orig_w, orig_h = base.size
    
    # Target frame resolution: 1280x720 for crisp HD playback and optimal performance
    target_w, target_h = 1280, 720
    
    # Total frames in seamless loop: 96 frames @ 24fps = 4.0 seconds
    num_frames = 96
    fps = 24
    
    out_dir = "data/assets/video_frames"
    os.makedirs(out_dir, exist_ok=True)
    
    # Pre-generate 35 deterministic floating particles for seamless looping
    # Each particle has: (base_x, base_y, speed_y, size, alpha, drift_phase)
    random.seed(42)
    particles = []
    for _ in range(40):
        particles.append({
            'bx': random.uniform(50, target_w - 50),
            'by': random.uniform(50, target_h - 50),
            'speed': random.uniform(40, 90),
            'size': random.uniform(2.0, 5.0),
            'color': random.choice([
                (56, 189, 248),   # Cyan-400
                (129, 140, 248),  # Indigo-400
                (52, 211, 153),   # Emerald-400
                (251, 191, 36),   # Amber-400
            ]),
            'phase': random.uniform(0, 2 * math.pi)
        })

    print(f"Generating {num_frames} frames ({target_w}x{target_h})...")
    start_time = time.time()
    
    for i in range(num_frames):
        t = i / float(num_frames) # 0.0 to 1.0
        angle = t * 2 * math.pi   # 0 to 2*PI for perfect seamless loop
        
        # 1. Smooth Camera Motion (gentle zoom 1.06 to 1.13 and subtle horizontal pan)
        # Using sin/cos ensures frame 0 matches frame 96 perfectly
        zoom = 1.08 + 0.05 * math.sin(angle)
        scaled_w = int(orig_w * zoom)
        scaled_h = int(orig_h * zoom)
        scaled = base.resize((scaled_w, scaled_h), Image.Resampling.BILINEAR)
        
        # Pan horizontally across checkout desk: basket (left) <-> scanner/pos (center) <-> printer (right)
        pan_x = 0.5 + 0.28 * math.sin(angle)
        pan_y = 0.5 + 0.08 * math.cos(angle)
        
        crop_x = int((scaled_w - target_w) * pan_x)
        crop_y = int((scaled_h - target_h) * pan_y)
        crop_x = max(0, min(scaled_w - target_w, crop_x))
        crop_y = max(0, min(scaled_h - target_h, crop_y))
        
        frame = scaled.crop((crop_x, crop_y, crop_x + target_w, crop_y + target_h))
        
        # 2. Overlay Layer for Dynamic Video Effects (Light Sweeps, Laser, Particles)
        overlay = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
        
        # A. Scanner Laser Pulse Effect
        # Coordinates roughly around scanner laser beam in checkout desk
        laser_progress = 0.5 + 0.5 * math.sin(angle * 2) # sweeps twice per loop
        laser_alpha = int(45 + 35 * math.sin(angle * 4))
        laser_y = int(target_h * 0.42 + 40 * math.sin(angle * 2))
        laser_x_start = int(target_w * 0.40)
        laser_x_end = int(target_w * 0.58)
        
        # Draw soft glowing fan / beam
        beam_poly = [
            (int(target_w * 0.54), int(target_h * 0.40)), # Scanner nozzle
            (int(laser_x_start), int(laser_y + 30)),
            (int(laser_x_end), int(laser_y + 40))
        ]
        draw.polygon(beam_poly, fill=(56, 189, 248, int(laser_alpha * 0.35)))
        # Thin core beam
        draw.line([
            (int(target_w * 0.54), int(target_h * 0.40)),
            (int(laser_x_start + (laser_x_end - laser_x_start) * laser_progress), int(laser_y + 35))
        ], fill=(165, 243, 252, laser_alpha), width=2)
        
        # B. Floating Cyber Dust / Particles (Drifting upwards with loop wrap)
        for p in particles:
            # Shift Y upwards smoothly over time, wrap around target_h
            py = (p['by'] - p['speed'] * t * (target_h / 80.0)) % target_h
            px = p['bx'] + 15 * math.sin(angle + p['phase'])
            
            # Twinkle brightness
            twinkle = 0.6 + 0.4 * math.sin(angle * 3 + p['phase'])
            r, g, b = p['color']
            p_alpha = int(140 * twinkle)
            sz = p['size'] * (0.8 + 0.2 * twinkle)
            
            draw.ellipse([px - sz, py - sz, px + sz, py + sz], fill=(r, g, b, p_alpha))
            # Soft outer glow
            draw.ellipse([px - sz * 2, py - sz * 2, px + sz * 2, py + sz * 2], fill=(r, g, b, int(p_alpha * 0.25)))
        
        # C. Ambient Light Sheen / Glass Sweep
        # A soft light wave traveling diagonally across the scene
        sheen_x = int((t * 2.0 % 1.0) * (target_w + 400) - 200)
        sheen_poly = [
            (sheen_x, 0),
            (sheen_x + 120, 0),
            (sheen_x - 100, target_h),
            (sheen_x - 220, target_h)
        ]
        sheen_alpha = int(16 + 8 * math.sin(angle))
        draw.polygon(sheen_poly, fill=(255, 255, 255, sheen_alpha))
        
        # Merge overlay onto frame
        combined = Image.alpha_composite(frame, overlay)
        
        # Save as optimized JPEG
        frame_file = os.path.join(out_dir, f"frame_{i:03d}.jpg")
        combined.convert("RGB").save(frame_file, quality=88)
        
        if (i + 1) % 24 == 0 or i == num_frames - 1:
            print(f"Generated frame {i+1}/{num_frames}")

    elapsed = time.time() - start_time
    print(f"All {num_frames} frames generated in {elapsed:.2f} seconds.")
    
    # Encode into login_video.mp4 using ffmpeg
    mp4_out = "data/assets/login_video.mp4"
    print(f"Encoding {mp4_out} with ffmpeg...")
    cmd = [
        "ffmpeg", "-y",
        "-framerate", str(fps),
        "-i", os.path.join(out_dir, "frame_%03d.jpg"),
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-crf", "18",
        "-preset", "fast",
        mp4_out
    ]
    subprocess.run(cmd, check=True)
    print(f"Successfully generated {mp4_out} ({os.path.getsize(mp4_out) / 1024:.1f} KB)")

if __name__ == "__main__":
    generate_video_loop()

