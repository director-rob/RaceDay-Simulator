import processing.core.PApplet;
import processing.core.PImage;

public class AidStation {
    PApplet p;
    float x, y;
    float staminaRecovery = 20;
    float hydrationRecovery = 30;
    boolean used = false;
    PImage aidStationImg;


    public AidStation(PApplet p, float x, float y) {
        this.p = p;
        this.x = x;
        this.y = y;
        this.aidStationImg = p.loadImage("aidstation.png");
        if (aidStationImg == null) {
            System.out.println("Error: aidstation.png not loaded!");
        }
    }

    public void display() {
        if (!used) {
            if (aidStationImg != null) {
                p.image(aidStationImg, x, y, 40, 40); // Display the image
            } else {
                p.fill(0, 255, 0);
                p.rect(x, y, 40, 40); // Fallback if the image is not loaded
            }
        }
    }

    public boolean checkCollision(PlayerRunner player) {
        return !used && PApplet.dist(player.x, player.y, x + 20, y + 20) < 30;
    }

    public void use(PlayerRunner player) {
        if (!used) {
            player.stamina = Math.min(player.stamina + staminaRecovery, 100);
            player.hydration = Math.min(player.hydration + hydrationRecovery, 100);
            used = true;
        }
    }
}