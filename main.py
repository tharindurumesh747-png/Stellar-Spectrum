import pygame
import sys
import os

# Android-specific setup
try:
    from android.permissions import request_permissions, Permission
    request_permissions([Permission.VIBRATE])
    ANDROID = True
except ImportError:
    ANDROID = False

# Screen size
SCREEN_W = 1080
SCREEN_H = 1920

def main():
    pygame.init()
    pygame.mixer.init()

    if ANDROID:
        screen = pygame.display.set_mode((0, 0), pygame.FULLSCREEN)
        SCREEN_W, SCREEN_H = screen.get_size()
    else:
        screen = pygame.display.set_mode((540, 960))

    pygame.display.set_caption("Stellar Spectrum")
    clock = pygame.time.Clock()

    # Colors
    BLACK  = (2, 5, 16)
    CYAN   = (0, 207, 255)
    WHITE  = (255, 255, 255)
    PURPLE = (191, 95, 255)
    GREEN  = (57, 255, 122)
    RED    = (255, 51, 102)
    GOLD   = (255, 215, 0)

    SW, SH = screen.get_size()
    font_big   = pygame.font.SysFont("monospace", int(SH * 0.06), bold=True)
    font_med   = pygame.font.SysFont("monospace", int(SH * 0.03))
    font_small = pygame.font.SysFont("monospace", int(SH * 0.022))

    # Simple starfield
    import random
    stars = [(random.randint(0, SW), random.randint(0, SH),
              random.uniform(0.3, 1.5)) for _ in range(200)]

    star_offset = 0.0
    tick = 0

    running = True
    while running:
        dt = clock.tick(60) / 1000.0
        tick += 1
        star_offset = (star_offset + 1.2) % SH

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    running = False
            if event.type == pygame.FINGERDOWN or event.type == pygame.MOUSEBUTTONDOWN:
                pass  # placeholder for future screen transitions

        # Draw background
        screen.fill(BLACK)

        # Draw scrolling stars
        for sx, sy, spd in stars:
            y = (sy + star_offset * spd) % SH
            r = max(1, int(spd))
            pygame.draw.circle(screen, WHITE, (sx, int(y)), r)

        # Glow title — "STELLAR SPECTRUM"
        pulse = abs((tick % 120) - 60) / 60.0  # 0.0 → 1.0
        glow_alpha = int(180 + 75 * pulse)
        colors_cycle = [RED, CYAN, GREEN, PURPLE, GOLD]
        title_text = "STELLAR SPECTRUM"
        char_w = int(SW * 0.055)
        start_x = SW // 2 - (len(title_text) * char_w) // 2
        for i, ch in enumerate(title_text):
            col = colors_cycle[i % len(colors_cycle)]
            surf = font_big.render(ch, True, col)
            screen.blit(surf, (start_x + i * char_w, int(SH * 0.28)))

        # Tagline
        tag = font_med.render("COLOR  ·  SHOOT  ·  SURVIVE", True, CYAN)
        screen.blit(tag, (SW // 2 - tag.get_width() // 2, int(SH * 0.38)))

        # Animated color chips
        chip_colors = [RED, CYAN, GREEN, PURPLE]
        chip_labels = ["RED PLASMA", "BLUE QUANTUM", "GREEN NOVA", "PURPLE VOID"]
        chip_y = int(SH * 0.50)
        chip_w = int(SW * 0.40)
        chip_h = int(SH * 0.055)
        chip_gap = int(SH * 0.068)
        for i, (cc, cl) in enumerate(zip(chip_colors, chip_labels)):
            cx = SW // 2 - chip_w // 2
            cy = chip_y + i * chip_gap
            # Panel
            panel = pygame.Surface((chip_w, chip_h), pygame.SRCALPHA)
            panel.fill((*cc, 30))
            screen.blit(panel, (cx, cy))
            pygame.draw.rect(screen, cc, (cx, cy, chip_w, chip_h), 2, border_radius=8)
            # Dot
            pygame.draw.circle(screen, cc, (cx + int(chip_h * 0.5), cy + chip_h // 2), 8)
            # Label
            lbl = font_small.render(cl, True, cc)
            screen.blit(lbl, (cx + int(chip_h * 0.9), cy + chip_h // 2 - lbl.get_height() // 2))

        # Tap to start
        if tick % 90 < 60:
            tap = font_med.render("TAP TO START", True, WHITE)
            screen.blit(tap, (SW // 2 - tap.get_width() // 2, int(SH * 0.86)))

        # Version
        ver = font_small.render("v1.0.0  |  Stellar Spectrum", True, (60, 90, 120))
        screen.blit(ver, (SW // 2 - ver.get_width() // 2, int(SH * 0.96)))

        pygame.display.flip()

    pygame.quit()
    sys.exit()

if __name__ == "__main__":
    main()
