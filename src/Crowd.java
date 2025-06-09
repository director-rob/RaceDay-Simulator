import processing.core.PApplet;
import processing.core.PImage;

public class Crowd {
    PApplet p;
    float x, y;
    float speed;  // How fast the crowd moves across the screen.
    PImage crowdImg; // Field to store the crowd image.

    public Crowd(PApplet p, float startX, float y, float speed) {
        this.p = p;
        this.x = startX;
        this.y = y;
        this.speed = speed;
        // Load the crowd image.
        this.crowdImg = p.loadImage("crowd.png");
        if (crowdImg == null) {
            System.out.println("Error: crowd.png not loaded!");
        }
    }

    public boolean isOffScreen() {
        return x + 1000 < 0; // Adjust 1000 to match the width of the crowd image.
    }

    public void update() {
        // Move left across the screen.
        x -= speed;
    }

    public void display() {
        p.pushStyle();
        if (crowdImg != null) {
            // Draw the crowd image.
            p.image(crowdImg, x, y, 1000, 300);
        } else {
            // Fallback: Draw a rectangle if the image is not loaded.
            p.fill(100);
            p.noStroke();
            p.rect(x, y, 100, 50);
        }
        p.popStyle();
    }
}