from kivy.app import App
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.uix.widget import Widget
from kivy.graphics import Color, Ellipse, Rectangle, Line
from kivy.clock import Clock
from kivy.core.window import Window
import random
import math

Window.clearcolor = (0.008, 0.02, 0.063, 1)  # Deep space dark


class StarfieldWidget(Widget):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.stars = []
        self.time = 0
        Clock.schedule_once(self._init_stars, 0.1)
        Clock.schedule_interval(self.update, 1 / 60)

    def _init_stars(self, dt):
        self.stars = [
            [random.random(), random.random(), random.uniform(0.5, 2.5)]
            for _ in range(200)
        ]
        self.draw_scene()

    def update(self, dt):
        self.time += dt
        for s in self.stars:
            s[1] -= s[2] * dt * 0.08
            if s[1] < 0:
                s[1] = 1.0
                s[0] = random.random()
        self.draw_scene()

    def draw_scene(self):
        self.canvas.clear()
        w, h = self.size
        if w == 0:
            return

        with self.canvas:
            # Stars
            for sx, sy, spd in self.stars:
                bright = min(1.0, spd / 2.5)
                Color(bright, bright, bright, 1)
                r = max(1, spd * 0.8)
                Ellipse(pos=(sx * w - r, sy * h - r), size=(r * 2, r * 2))

            # Animated color orbs
            colors = [
                (1, 0.2, 0.4),    # Red
                (0, 0.81, 1),     # Cyan
                (0.22, 1, 0.48),  # Green
                (0.75, 0.37, 1),  # Purple
            ]
            for i, col in enumerate(colors):
                angle = self.time * 0.5 + i * math.pi / 2
                ox = w * 0.5 + math.cos(angle) * w * 0.28
                oy = h * 0.42 + math.sin(angle * 0.7) * h * 0.06
                Color(*col, 0.15)
                Ellipse(pos=(ox - 60, oy - 60), size=(120, 120))
                Color(*col, 0.4)
                Ellipse(pos=(ox - 12, oy - 12), size=(24, 24))

            # Ship shape at bottom center
            cx = w * 0.5
            cy = h * 0.15
            pulse = 0.8 + 0.2 * math.sin(self.time * 3)
            Color(0, 0.81, 1, pulse)
            # Ship body triangle
            Line(points=[
                cx, cy + 50,
                cx - 30, cy - 20,
                cx + 30, cy - 20,
                cx, cy + 50
            ], width=2)
            # Engine glow
            Color(0.22, 1, 0.48, 0.6 * pulse)
            Ellipse(pos=(cx - 8, cy - 30), size=(16, 16))


class StellarSpectrumApp(App):
    def build(self):
        layout = FloatLayout()

        # Starfield background
        starfield = StarfieldWidget(
            size_hint=(1, 1),
            pos_hint={'x': 0, 'y': 0}
        )
        layout.add_widget(starfield)

        # Title
        title = Label(
            text='STELLAR\nSPECTRUM',
            font_size='52sp',
            bold=True,
            color=(0, 0.81, 1, 1),
            halign='center',
            size_hint=(1, None),
            height=160,
            pos_hint={'center_x': 0.5, 'center_y': 0.75}
        )
        layout.add_widget(title)

        # Tagline
        tagline = Label(
            text='COLOR  ·  SHOOT  ·  SURVIVE',
            font_size='18sp',
            color=(0.75, 0.37, 1, 0.9),
            size_hint=(1, None),
            height=40,
            pos_hint={'center_x': 0.5, 'center_y': 0.65}
        )
        layout.add_widget(tagline)

        # Color chips
        chip_texts = ['🔴 RED PLASMA', '🔵 BLUE QUANTUM',
                      '🟢 GREEN NOVA', '🟣 PURPLE VOID']
        chip_y = [0.52, 0.46, 0.40, 0.34]
        for text, cy in zip(chip_texts, chip_y):
            lbl = Label(
                text=text,
                font_size='16sp',
                color=(0.8, 0.9, 1, 0.85),
                size_hint=(0.6, None),
                height=36,
                pos_hint={'center_x': 0.5, 'center_y': cy}
            )
            layout.add_widget(lbl)

        # Tap to start
        tap = Label(
            text='TAP TO START',
            font_size='20sp',
            bold=True,
            color=(1, 1, 1, 0.9),
            size_hint=(1, None),
            height=40,
            pos_hint={'center_x': 0.5, 'center_y': 0.08}
        )
        layout.add_widget(tap)

        # Version
        ver = Label(
            text='v1.0.0  |  Stellar Spectrum',
            font_size='12sp',
            color=(0.3, 0.45, 0.6, 0.7),
            size_hint=(1, None),
            height=30,
            pos_hint={'center_x': 0.5, 'center_y': 0.02}
        )
        layout.add_widget(ver)

        return layout


if __name__ == '__main__':
    StellarSpectrumApp().run()
