import processing.core.PApplet;
import processing.core.PImage;

public class PlayerRunner {

    PApplet p;
    float x, y;
    float stamina = 100, hydration = 100, gutTolerance = 100, maxStamina = 100;
    boolean cramped = false;
    int gels = 10;

    PImage[] runnerFrames;
    int currentFrame = 0;
    int frameDelay = 5;
    int frameCounter = 0;

    String playerClass = "Novice Runner";  // Default value;

    // New variable to track the last gel usage time in ms.
    long lastGelUsageTime = 0;

    public PlayerRunner(PApplet p) {
        this.p = p;
        this.x = 100;
        this.y = p.height / 2;
        runnerFrames = new PImage[5];
        for (int i = 0; i < 5; i++) {
            runnerFrames[i] = p.loadImage("frame" + (i + 1) + ".png");
        }
    }

    // Setter method for player class.
    public void setPlayerClass(String className) {
        this.playerClass = className;
        if (className.equals("Elite Runner")) {
            maxStamina = 150;
        } else if (className.equals("Balanced Runner")) {
            maxStamina = 120;
        } else {  // "Novice Runner"
            maxStamina = 100;
        }
    }

    public void update(float speed) {
        // Base stamina decay based on current speed.
        float baseDecay = speed * 0.005f;
        // Determine the optimal speed (running at or below this speed is "good").
        float optimalSpeed = 5.0f;  // Adjust this threshold as needed.
        float excess = Math.max(speed - optimalSpeed, 0);

        // Extra decay kicks in if running too fast.
        float extraDecay;
        if (playerClass.equals("Elite Runner")) {
            extraDecay = excess * 0f;
        } else if (playerClass.equals("Balanced Runner")) {
            extraDecay = excess * 0.05f;
        } else {  // "Novice Runner"
            extraDecay = excess * 0.1f;
        }

        // Deduct stamina with the combined decay rate.
        stamina -= (baseDecay + extraDecay);
        if (stamina < 0) {
            stamina = 0;
        }

        // Hydration depletion
        hydration -= speed * 0.01f;
        if (hydration < 0) {
            hydration = 0;
        }

        // Regenerate gutTolerance slowly over time.
        gutTolerance += 0.1f; // Regenerate 0.1 points per frame
        if (gutTolerance > 100) {
            gutTolerance = 100;
        }

        checkCrampAndGutIssues();

        frameCounter++;
        if (frameCounter >= frameDelay) {
            currentFrame = (currentFrame + 1) % runnerFrames.length;
            frameCounter = 0;
        }
    }

    public void updateAdrenaline(float speed) {
        // In adrenaline mode, update only the animation frames, no stamina or hydration changes.
        frameCounter++;
        if (frameCounter >= frameDelay) {
            currentFrame = (currentFrame + 1) % runnerFrames.length;
            frameCounter = 0;
        }
    }

    // Check if hydration or stamina fall below thresholds.
    private void checkCrampAndGutIssues() {
        // Mark as cramped if either parameter is very low.
        if (hydration < 15 || stamina < 10) {
            cramped = true;
        } else {
            cramped = false;
        }
        // Apply an extra penalty when gutTolerance is very low.
        if (gutTolerance < 20) {
            stamina -= 0.05f * 5;
            if (stamina < 0) {
                stamina = 0;
            }
        }
    }

    public void display() {
        PImage currentFrameImage = runnerFrames[currentFrame];
        float offsetX = currentFrameImage.width / 2.0f;
        float offsetY = currentFrameImage.height;
        p.image(currentFrameImage, x - offsetX, y - offsetY);
    }


    public boolean isCramped() {
        return cramped;
    }

    public boolean hasGel() {
        return gels > 0 && gutTolerance > 15;
    }

    // Using a gel recovers stamina and reduces gutTolerance.
    // Implements increased penalty for rapid gel usage.
    public void useGel() {
        if (hasGel()) {
            gels--;
            // Get current time.
            long currentTime = p.millis();
            // Base gutTolerance reduction.
            float reduction = 5;
            // If the last gel was used less than 5 seconds ago, increase the reduction by 50%.
            if (currentTime - lastGelUsageTime < 5000) {
                reduction *= 1.5f;
            }
            lastGelUsageTime = currentTime;
            gutTolerance -= reduction;
            if (gutTolerance < 0) {
                gutTolerance = 0;
            }
            // Recover stamina (cap to maxStamina).
            stamina = Math.min(stamina + 40, maxStamina);
            cramped = false;
        }
    }
    public PImage getPlayerImg() {
        return runnerFrames[currentFrame];
    }
    public void applyPenalty() {
        stamina = Math.max(stamina - 10, 0);
        hydration = Math.min(hydration + 10, 100);
        cramped = false;
    }
}
