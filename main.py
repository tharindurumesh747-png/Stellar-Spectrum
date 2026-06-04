# 1. KIVY CONFIGURATION & LOW-END HARDWARE OPTIMIZATIONS (MUST BE AT VERY TOP)
from kivy.config import Config
Config.set('graphics', 'width', '720')
Config.set('graphics', 'height', '1520')
Config.set('graphics', 'resizable', '0')
Config.set('graphics', 'fullscreen', '1') # Force immersive layout on mobile
Config.set('graphics', 'provider', 'sdl2') # Use highly efficient SDL2 backend
Config.set('input', 'mouse', 'mouse,disable_multitouch') # Turn off touch emulations for CPU efficiency

import random
from kivy.app import App
from kivy.uix.widget import Widget
from kivy.properties import NumericProperty, ReferenceListProperty, ObjectProperty, ListProperty, StringProperty
from kivy.vector import Vector
from kivy.clock import Clock
from kivy.graphics import Color, Ellipse, Rectangle, Line, InstructionGroup, Quad
from kivy.core.window import Window
from kivy.uix.label import Label
from kivy.utils import get_color_from_hex

# Standard Vivo Y93 display dimension configuration references
TARGET_WIDTH = 720
TARGET_HEIGHT = 1520

class CosmicStar:
    """Lightweight, CPU-friendly scrolling star model representing distant celestial space."""
    def __init__(self, x, y, speed, size, color):
        self.x = x
        self.y = y
        self.speed = speed
        self.size = size
        self.color = color

class AlienDebris:
    """An obstacle or planetary debris that cascades down with rotating visual characteristics."""
    def __init__(self, x, y, radius, vx, vy, color, hp):
        self.x = x
        self.y = y
        self.radius = radius
        self.vx = vx
        self.vy = vy
        self.color = color
        self.hp = hp
        self.max_hp = hp

class EnergyLaser:
    """Fast flying plasma projectiles emitted from user's majestic starfighter."""
    def __init__(self, x, y, vy, color):
        self.x = x
        self.y = y
        self.vy = vy
        self.color = color

class SpaceParticle:
    """Glow/Explosion burst particles that decay and scale in size to replace expensive textures."""
    def __init__(self, x, y, vx, vy, size, color, max_life=0.4):
        self.x = x
        self.y = y
        self.vx = vx
        self.vy = vy
        self.size = size
        self.color = color
        self.life = max_life
        self.max_life = max_life

class StellarSpectrumGame(Widget):
    """The core optimized game canvas widget managing rendering & state for Stellar Spectrum Kivy."""
    score = NumericProperty(0)
    combo = NumericProperty(1)
    shields = NumericProperty(100)
    level = NumericProperty(1)
    current_ship_id = StringProperty("omega_vanguard") # Procedural Majestic crescent style reference
    
    def __init__(self, **kwargs):
        super(StellarSpectrumGame, self).__init__(**kwargs)
        self.ship_x = TARGET_WIDTH / 2
        self.ship_y = 150
        self.ship_radius = 42
        
        # State Arrays
        self.stars = []
        self.debris_list = []
        self.lasers = []
        self.particles = []
        
        # Core game timings
        self.spawn_timer = 0
        self.combo_timer = 0
        self.game_over = False
        
        # Build initial scrolling star lists (3 layered depth parallax)
        for i in range(45):
            self.stars.append(CosmicStar(
                random.uniform(0, TARGET_WIDTH),
                random.uniform(0, TARGET_HEIGHT),
                random.uniform(60, 240), # depth speed
                random.uniform(1.5, 3.5), # star size
                random.choice(["#FFFFFF", "#818CF8", "#38BDF8", "#F472B6"])
            ))
            
        # Hook global touch window bounds
        Window.bind(on_touch_down=self.intercept_global_touch)
        Window.bind(on_touch_move=self.intercept_global_move)
        
        # Game Loops optimized for low-end platforms at 60Hz max (scheduled updates)
        Clock.schedule_interval(self.tick_physics, 1.0 / 60.0)
        
        # Title Card Label Overlay
        self.title_label = Label(
            text="STELLAR SPECTRUM",
            font_size="24sp",
            bold=True,
            color=get_color_from_hex("#FBBF24"),
            pos=(0, TARGET_HEIGHT - 100),
            size_hint=(1, None),
            height=50
        )
        self.add_widget(self.title_label)
        
        # Status Label Info
        self.hud_label = Label(
            text="Shields: 100% | Score: 0000 | Multiplier: 1x",
            font_size="14sp",
            pos=(0, TARGET_HEIGHT - 150),
            size_hint=(1, None),
            height=30
        )
        self.add_widget(self.hud_label)
        
        self.info_label = Label(
            text="[ Drag/Slide at bottom to fly! Tap high to fire. ]",
            font_size="12sp",
            color=get_color_from_hex("#94A3B8"),
            pos=(0, 20),
            size_hint=(1, None),
            height=30
        )
        self.add_widget(self.info_label)

    def intercept_global_touch(self, window, touch):
        if self.game_over:
            # Tap to reset
            self.reset_game()
            return True
            
        # If touch originates in bottom zone, slide ship there immediately
        if touch.y < 350:
            self.ship_x = max(30, min(TARGET_WIDTH - 30, touch.x))
        else:
            # Tap in active space fires weapons!
            self.trigger_laser_weapon()
        return True

    def intercept_global_move(self, window, touch):
        # Continuous ship sliding
        if touch.y < 350:
            self.ship_x = max(30, min(TARGET_WIDTH - 30, touch.x))
        return True

    def trigger_laser_weapon(self):
        """Discharges custom dual cosmic lasers depending on current ship attributes."""
        # Laser dual configs offset symmetrically
        color_hex = "#F43F5E" if self.combo > 1 else "#38BDF8"
        self.lasers.append(EnergyLaser(self.ship_x - 16, self.ship_y + 15, 950, color_hex))
        self.lasers.append(EnergyLaser(self.ship_x + 16, self.ship_y + 15, 950, color_hex))
        
        # Gentle recoil particles
        for _ in range(3):
            self.particles.append(SpaceParticle(
                self.ship_x, self.ship_y + 10,
                random.uniform(-40, 40),
                random.uniform(-30, -80),
                random.uniform(4, 8),
                "#38BDF8"
            ))

    def trigger_particle_burst(self, x, y, num_particles, color):
        """Creates highly efficient explosive particles to signify debris destruction."""
        for _ in range(num_particles):
            self.particles.append(SpaceParticle(
                x, y,
                random.uniform(-350, 350),
                random.uniform(-350, 350),
                random.uniform(6, 14),
                color,
                max_life=random.uniform(0.2, 0.45)
            ))

    def reset_game(self):
        self.score = 0
        self.combo = 1
        self.shields = 100
        self.level = 1
        self.debris_list.clear()
        self.lasers.clear()
        self.particles.clear()
        self.game_over = False
        self.title_label.text = "STELLAR SPECTRUM"
        self.title_label.color = get_color_from_hex("#FBBF24")

    def tick_physics(self, dt):
        """Updates physics ticks. Clamped delta-time maintains simulation consistency."""
        dt = min(dt, 0.05) # Prevent physics leaps on lag frames
        
        if self.game_over:
            return
            
        # Update combo multipliers decay
        if self.combo > 1:
            self.combo_timer -= dt
            if self.combo_timer <= 0:
                self.combo = max(1, self.combo - 1)
                self.combo_timer = 2.0
                
        # Stars parallax scroll
        for star in self.stars:
            star.y -= star.speed * dt
            if star.y < 0:
                star.y = TARGET_HEIGHT
                star.x = random.uniform(0, TARGET_WIDTH)
                
        # Debris spawning
        self.spawn_timer += dt
        spawn_interval = max(0.4, 2.0 - (self.level * 0.15))
        if self.spawn_timer >= spawn_interval:
            self.spawn_timer = 0
            size_rank = random.choice([1, 2, 3]) # size ranks
            radius = 20 * size_rank
            hp = size_rank * 2
            debris_color = "#EA580C" if size_rank == 3 else ("#FBBF24" if size_rank == 2 else "#10B981")
            self.debris_list.append(AlienDebris(
                random.uniform(radius, TARGET_WIDTH - radius),
                TARGET_HEIGHT + 30,
                radius,
                random.uniform(-100, 100) if size_rank == 1 else random.uniform(-40, 40),
                random.uniform(-180, -320) - (self.level * 18),
                debris_color,
                hp
            ))
            
        # Tick Lasers
        for laser in list(self.lasers):
            laser.y += laser.vy * dt
            if laser.y > TARGET_HEIGHT + 20:
                self.lasers.remove(laser)
                
        # Tick Particles
        for part in list(self.particles):
            part.x += part.vx * dt
            part.y += part.vy * dt
            part.life -= dt
            if part.life <= 0:
                self.particles.remove(part)
                
        # Tick Debris
        for db in list(self.debris_list):
            db.x += db.vx * dt
            db.y += db.vy * dt
            
            # Clamp limits horizontally & reverse directions on wall hits
            if db.x < db.radius or db.x > TARGET_WIDTH - db.radius:
                db.vx = -db.vx
                
            # Crash to bottom screen checks
            if db.y < -50:
                self.debris_list.remove(db)
                self.shields = max(0, self.shields - 15)
                self.combo = 1
                if self.shields <= 0:
                    self.game_over = True
                    self.title_label.text = "💥 SYSTEM DEVIATION DETECTED!"
                    self.title_label.color = get_color_from_hex("#EF4444")
                    
            # Ship Collision checks
            dist_ship = Vector(db.x, db.y).distance(Vector(self.ship_x, self.ship_y))
            if dist_ship < (db.radius + self.ship_radius - 5):
                self.trigger_particle_burst(db.x, db.y, 16, db.color)
                self.shields = max(0, self.shields - (db.radius // 2))
                self.combo = 1
                try:
                    self.debris_list.remove(db)
                except ValueError:
                    pass
                if self.shields <= 0:
                    self.game_over = True
                    self.title_label.text = "💥 CRITICAL IMPACT: CORE DOWN"
                    self.title_label.color = get_color_from_hex("#EF4444")
                    
        # Laser and Debris Overlap checking
        for laser in list(self.lasers):
            for db in list(self.debris_list):
                dist = Vector(laser.x, laser.y).distance(Vector(db.x, db.y))
                if dist < (db.radius + 12):
                    # Trigger impact particle spark
                    self.particles.append(SpaceParticle(
                        laser.x, laser.y,
                        random.uniform(-100, 100),
                        random.uniform(50, 150),
                        6.0,
                        laser.color,
                        0.2
                    ))
                    
                    try:
                        self.lasers.remove(laser)
                    except ValueError:
                        pass
                        
                    db.hp -= 1
                    if db.hp <= 0:
                        self.trigger_particle_burst(db.x, db.y, 14, db.color)
                        # Award pointsscaled accurately
                        self.score += db.max_hp * 10 * self.combo
                        self.combo = min(5, self.combo + 1)
                        self.combo_timer = 2.5
                        
                        try:
                            self.debris_list.remove(db)
                        except ValueError:
                            pass
                            
                        # Level scaling threshold check
                        if self.score > self.level * 450:
                            self.level += 1
                    break # Break inner debris loop search for this specific laser

        # Redraw Kivy canvas layout dynamically
        self.redraw_game_canvas()
        self.hud_label.text = f"Shields: {self.shields}% | Score: {self.score:04d} | Mult: {self.combo}x"

    def redraw_game_canvas(self):
        """Draws complex vector art. Bypasses image requests to run at higher framerates."""
        self.canvas.clear()
        with self.canvas:
            # Draw Dynamic Space Background Slate (Midnight Space theme)
            Color(0.04, 0.04, 0.08, 1) # deep cosmic theme background
            Rectangle(pos=self.pos, size=(TARGET_WIDTH, TARGET_HEIGHT))
            
            # Render stars first
            for star in self.stars:
                rgba = get_color_from_hex(star.color)
                Color(rgba[0], rgba[1], rgba[2], 0.75)
                Ellipse(pos=(star.x - star.size / 2, star.y - star.size / 2), size=(star.size, star.size))
                
            # Render Active Lasers
            for laser in self.lasers:
                rgba = get_color_from_hex(laser.color)
                Color(rgba[0], rgba[1], rgba[2], 1.0)
                # Thick high energy plasma capsule
                Line(points=[laser.x, laser.y - 12, laser.x, laser.y + 12], width=3.2)
                Color(1, 1, 1, 0.8)
                Line(points=[laser.x, laser.y - 6, laser.x, laser.y + 6], width=1.2)
                
            # Render alien asteroids/debris
            for db in self.debris_list:
                # Radial core rendering
                rgba = get_color_from_hex(db.color)
                Color(rgba[0], rgba[1], rgba[2], 0.95)
                Ellipse(pos=(db.x - db.radius, db.y - db.radius), size=(db.radius * 2, db.radius * 2))
                
                # Dynamic orbital protection vector ring (similar to our Compose rings)
                Color(rgba[0]/2, rgba[1]/2, rgba[2]/2, 0.4)
                Line(circle=(db.x, db.y, db.radius + 6), width=1.5)
                
                # Core life tracker indicators
                if db.hp < db.max_hp:
                    hp_pct = max(0.0, db.hp / db.max_hp)
                    Color(1, 0, 0, 0.8) # red bar
                    Line(points=[db.x - 15, db.y + db.radius + 8, db.x + 15, db.y + db.radius + 8], width=2.5)
                    Color(0, 1, 0, 0.85) # green fill
                    Line(points=[db.x - 15, db.y + db.radius + 8, db.x - 15 + (30 * hp_pct), db.y + db.radius + 8], width=2.5)
                    
            # Render decay explosive particles
            for part in self.particles:
                rgba = get_color_from_hex(part.color)
                alpha = part.life / part.max_life
                Color(rgba[0], rgba[1], rgba[2], alpha)
                Ellipse(pos=(part.x - part.size / 2, part.y - part.size / 2), size=(part.size, part.size))
                
            # Render the User's Majestic Starfighter (Optimized crescent shape vector logic)
            if not self.game_over:
                # 1. Jet Engine fire flares particle streams
                boost_length = 30 + random.uniform(0, 20)
                Color(1.0, 0.6, 0.0, 0.9) # fire orange color
                Line(points=[
                    self.ship_x, self.ship_y - self.ship_radius * 1.1,
                    self.ship_x, self.ship_y - self.ship_radius * 1.1 - boost_length
                ], width=4.5)
                Color(1.0, 0.9, 0.2, 0.9) # core yellow color
                Line(points=[
                    self.ship_x, self.ship_y - self.ship_radius * 1.1,
                    self.ship_x, self.ship_y - self.ship_radius * 1.1 - boost_length * 0.6
                ], width=2.0)
                
                # 2. Outer Crescent Shields / Wing rings
                hull_color = get_color_from_hex("#818CF8")
                Color(hull_color[0], hull_color[1], hull_color[2], 1.0)
                
                # Draw high intensity curved crescent arcs for wings
                Line(circle=(self.ship_x, self.ship_y, self.ship_radius), angle_start=240, angle_end=300, width=5.0)
                Line(circle=(self.ship_x, self.ship_y, self.ship_radius), angle_start=60, angle_end=120, width=5.0)
                
                # Symmetrical wing mounts
                Line(points=[
                    self.ship_x - self.ship_radius * 0.86, self.ship_y - self.ship_radius * 0.5,
                    self.ship_x - 12, self.ship_y + 10
                ], width=2.5)
                Line(points=[
                    self.ship_x + self.ship_radius * 0.86, self.ship_y - self.ship_radius * 0.5,
                    self.ship_x + 12, self.ship_y + 10
                ], width=2.5)
                
                # 3. Main Central cockpit pod
                Color(0.22, 0.24, 0.46, 1.0)
                Ellipse(pos=(self.ship_x - 16, self.ship_y - 20), size=(32, 40))
                
                # Glowing blue canopy
                Color(0, 0.9, 1.0, 0.85)
                Ellipse(pos=(self.ship_x - 8, self.ship_y - 4), size=(16, 20))
                
                # Nose cone needle
                Color(0.9, 0.9, 0.9, 1.0)
                Line(points=[
                    self.ship_x, self.ship_y + 20,
                    self.ship_x, self.ship_y + 35
                ], width=2.0)
            else:
                # Game Over custom display
                Color(0.95, 0.15, 0.25, 0.4)
                # Screen overlay tint indicators
                Rectangle(pos=self.pos, size=(TARGET_WIDTH, TARGET_HEIGHT))
                # Interactive reset text
                self.info_label.text = "[ GAME OVER! TAP ANYWHERE TO INITIATE RETURN SCAN ]"
                self.info_label.color = get_color_from_hex("#F87171")

class StellarSpectrumApp(App):
    """The Kivy Application subclass setting the core build loop parameters."""
    def build(self):
        # Enforce exact standard portrait configuration bounds of Y93 at start
        Window.size = (TARGET_WIDTH, TARGET_HEIGHT)
        game = StellarSpectrumGame()
        return game

if __name__ == '__main__':
    StellarSpectrumApp().run()
