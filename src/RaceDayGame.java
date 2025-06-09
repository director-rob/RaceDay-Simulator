import processing.core.PApplet;
import processing.core.PImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RaceDayGame extends PApplet {

    // Game states
    final int STATE_MENU = 0;
    final int STATE_SELECTION = 1;
    final int STATE_RUNNING = 2;
    final int STATE_CRAMPED = 3;
    final int STATE_FINAL_KICK = 4;
    final int STATE_FINISHED = 5;
    final int STATE_PAUSED = 6;
    final int STATE_SELECTION_COMPLETE = 7;
    final int HOW_TO_PLAY = 8;
    int gameState = STATE_MENU;

    PImage mainScreenImg;
    PlayerRunner player;
    UIManager ui;
    ArrayList<AidStation> aidStations = new ArrayList<>();
    ArrayList<RunnerLog> raceLog = new ArrayList<>();
    ArrayList<Integer> expectedBeats = new ArrayList<>();
    ArrayList<Integer> keyPressTimes = new ArrayList<>();
    int beatInterval = 333; // For selection challenge.
    int beatCount = 0;
    int totalBeats = 12;
    int score = 0;
    int lastBeatTime = 0;
    boolean beatActive = false;
    String feedback = "";

    // --- Ground Generation ---
    float groundTime = 0;           // Increases continuously.
    int groundResolution = 100;     // Sample every 100 pixels.
    float noiseMultiplier = 0.0002f; // Longer, slower-changing hills.
    float amplitude = 150;          // Height variation.
    float baseY;                    // 75% of screen height.

    // --- Speed & Distance / HUD Metrics ---
    float speed = 1;                // Default speed from slider.
    float speedToKmh = 2.2f;        // Conversion multiplier.
    float speedKmh = 0;
    float distanceTravelled = 0;    // In km.
    final float raceDistance = 42.2f;// Marathon distance.
    long gameStartTime = 0;         // Set when the game starts running.

    // --- Aid Event (Quick-Time) Parameters ---
    boolean aidEventActive = false;
    float aidEventStartTime = 0;
    float aidEventDuration = 3000;          // Base duration in ms.
    float aidOuterCircleStartRadius = 100;    // Starting outer radius of the ring.
    float aidInnerCircleRadius = 30;          // Inner circle radius.
    boolean aidEventHandled = false;          // To prevent double handling.
    float aidStationStartX;                   // Starting x position (offscreen-right).
    float lastAidEventDistance = 0;           // Last distance at which an event was triggered.
    float baseAidEventDistance = 0.5f;          // Base distance threshold (km).

    // --- Adrenaline Mode Parameters ---
    boolean adrenalineModeActive = false;
    float adrenalineModeDuration = 3000; // Lasts 3 seconds.
    float adrenalineModeStartTime = 0;
    float adrenalineSpeedBonus = 0;      // Extra speed bonus.
    float adrenalineBonusIncrement = 0.5f;
    float maxAdrenalineBonus = 10;         // Maximum bonus.

    // For scheduling adrenaline events (crowd events).
    float nextAdrenalineEventInterval = random(1000, 20000); // 1 to 20 sec.
    long lastAdrenalineEventTime = 0;
    Crowd currentCrowd = null;
    float crowdEventDuration = 2000; // Crowd event lasts 2 seconds.
    long crowdEventStartTime = 0;

    // --- Cramp Mode Fields ---
    boolean inCrampHop = false;
    float crampHopVelocity = 0;
    float gravity = 0.5f;  // Adjust for jump feel.
    float crampStartTime = 0;   // When cramp mode started.
    float hopBoost = 0;         // (Not used in this version; replaced by hop physics)
    float crampStartDistance = 0; // Distance when cramp mode begins.

    // --- DNF / Finish Screen Data ---
    float finalTimeSeconds = 0;
    String finishTimeString = "";
    float finishDistance = 0;
    float averagePace = 0;

    // --- Adrenaline Cheer Messages ---
    String[] cheers = {"Whoo!", "Go Runner!", "You're looking good!", "Keep it up!", "Fantastic!"};
    float lastCheerTime = 0;
    String currentCheer = "";
    float cheerX = 0, cheerY = 0;

    PImage aidEventImg;
    // --- Background ---
    PImage backgroundImg;
    float backgroundX = 0;
    long totalPausedTime = 0;
    long pauseStartTime = 0;

    public static void main(String[] args) {
        PApplet.main("RaceDayGame");
    }

    public void settings() {
        size(800, 600);
    }

    public void setup() {
        player = new PlayerRunner(this);
        mainScreenImg = loadImage("mainScreen.png");
        ui = new UIManager(this);
        frameRate(60);
        baseY = height * 0.75f;  // Road base.
        backgroundImg = loadImage("background.jpg");
        aidEventImg = loadImage("aidstation.png");
        lastAidEventDistance = distanceTravelled;
        lastAdrenalineEventTime = millis();
        nextAdrenalineEventInterval = random(1000, 20000);
    }

    public void draw() {
        background(255);
        switch (gameState) {
            case STATE_MENU:
                drawMenu();
                break;
            case STATE_SELECTION:
                updateSelectionChallenge();
                drawSelectionChallenge();
                break;
            case STATE_SELECTION_COMPLETE:
                drawSelectionCompleteScreen();
                break;
            case STATE_RUNNING:
                updateRace();
                drawRace();
                break;
            case STATE_CRAMPED:
                // In cramp mode, update only the hop physics and update background/distance.
                updateCrampMode();
                drawCrampScreen();  // drawRace() is called inside for background display.
                break;
            case STATE_FINAL_KICK:
                updateRace();
                drawFinalKick();
                break;
            case STATE_FINISHED:
                drawFinishScreen();
                break;
            case STATE_PAUSED:
                drawPauseScreen();
                break;
            case HOW_TO_PLAY:
                drawHowToPlayScreen();
                break;
            default:
                break;
        }
    }
    void drawHowToPlayScreen() {
            background(220);

            // Title
            fill(0);
            textAlign(CENTER, TOP);
            textSize(32);
            text("How To Play", width/2, 20);

            // Draw the gameplay screenshot (mainScreenImg) centered near the top.
            if(mainScreenImg != null) {
                // We display the screenshot in a bounding box (e.g. 400 x 300) centered on screen.
                image(mainScreenImg, width/2 - 200, 50, 400, 300);
            }
            pushMatrix();
            translate(0, -30); // Move down to make space for the text.
            // Instructions text below the screenshot.
            textSize(16);
            textAlign(LEFT, TOP);
            int textX = 50;
            int textY = 390;
            int lineSpacing = 24;
            text("1. Press 2 to start the selection challenge.", textX, textY);
            text("   In this challenge, press SPACE in time with the beat to set your runner's attributes.", textX, textY + lineSpacing);
            text("2. When the race starts, use the slider to adjust your speed and press G to use a gel.", textX, textY + 2*lineSpacing);
            text("3. Monitor your hydration and stamina. If hydration drops to zero, your runner enters cramp mode.", textX, textY + 3*lineSpacing);
            text("4. In cramp mode, press UP to hop. Your character will jump vertically,", textX, textY + 4*lineSpacing);
            text("   while the background moves horizontally based on your hop speed.", textX, textY + 5*lineSpacing);
            text("5. During an aid event, press SPACE at the right time as shown by the looping circle", textX, textY + 6*lineSpacing);
            text("   to recover hydration and resume running.", textX, textY + 7*lineSpacing);
            text("6. Press P at any time to pause the game.", textX, textY + 8*lineSpacing);
            text("Press H to return to the main menu.", textX, textY + 9*lineSpacing);

            // Draw the player image in the top-left corner
            if(player != null && player.getPlayerImg() != null) {
                image(player.getPlayerImg(), 10, 10, 80, 80);
            }
            popMatrix();
        }



    void drawCrampScreen() {
        drawRace();
        fill(255, 0, 0, 150);
        textAlign(CENTER, CENTER);
        textSize(32);
        text("Cramps! Press UP to hop!", width/2, height/2 +150);
        textSize(24);
    }

        void drawFinalKick() {
            drawRace(); // Reuse the race drawing logic.
            fill(255, 0, 0, 150);
            textAlign(CENTER, CENTER);
            textSize(32);
            text("Final Kick! Push to the finish!", width / 2, height / 2 - 150);
        }

    public void keyPressed() {
        // Aid Event Handling
        if (aidEventActive && key == ' ') {
            float rawProgress = (millis() - aidEventStartTime) / (float) aidEventDuration;
            float progress = rawProgress % 1.0f; // Looping progress value.
            if (abs(progress - 1.0f) < 0.15f) {
                feedback = "Aid: Perfect!";
                player.hydration = min(player.hydration + 25, 100);
                gameState = STATE_RUNNING; // Resume normal play.
            } else {
                feedback = "Aid: Miss!";
            }
            aidEventActive = false;
            lastAidEventDistance = distanceTravelled;
            return;
        }

        // --- Gel Mechanic: Press G to use a gel ---
        if (gameState == STATE_RUNNING && key == 'g') {
            if (player.hasGel()) {
                player.useGel();
                raceLog.add(new RunnerLog(frameCount, speed, player.stamina, player.hydration, "Used Gel"));
            }
        }

        // --- Adrenaline Mode Bonus Control ---
        if (adrenalineModeActive) {
            if (keyCode == UP) {
                adrenalineSpeedBonus += adrenalineBonusIncrement;
                if (adrenalineSpeedBonus > maxAdrenalineBonus)
                    adrenalineSpeedBonus = maxAdrenalineBonus;
            }
            if (keyCode == DOWN) {
                adrenalineSpeedBonus -= adrenalineBonusIncrement;
                if (adrenalineSpeedBonus < 0)
                    adrenalineSpeedBonus = 0;
            }
        }

        // In cramp mode, only initiate a hop if not already in a hop.
        if (gameState == STATE_CRAMPED && keyCode == UP) {
            if (!inCrampHop) {
                inCrampHop = true;
                crampHopVelocity = 10; // Set initial upward velocity.
                println("Cramp Mode Hop initiated, velocity: " + crampHopVelocity);
            }
        }

        // --- Pause/Resume ---
        if (gameState == STATE_RUNNING && key == 'p') {
            gameState = STATE_PAUSED;
            pauseStartTime = millis();
        } else if (gameState == STATE_PAUSED && key == 'p') {
            gameState = STATE_RUNNING;
            totalPausedTime += millis() - pauseStartTime;
        }

        // --- Other Key Handling ---
        if (key == '1') {
            gameState = STATE_MENU;
        }
        if (key == '2') {
            gameState = STATE_SELECTION;
            startSelectionChallenge();
        }
        if (key == '3') {
            gameState = STATE_RUNNING;
        }
        if (key == '4') {
        gameState = HOW_TO_PLAY;
        }

        if (gameState == STATE_RUNNING && key == 'g') {
            if (player.hasGel()) {
                player.useGel();
                raceLog.add(new RunnerLog(frameCount, speed, player.stamina, player.hydration, "Used Gel"));
            }
        }

        if (gameState == STATE_FINISHED && key == 'e') {
            println("Exporting logs and performing analysis...");

            // Sort by speed and export to file
            sortLogsBySpeed();
            exportLogToFile("sorted_by_speed.txt");

            // Search for hydration crashes and export
            ArrayList<RunnerLog> dehydrated = searchLogsByHydration(25);
            PrintWriter writer = createWriter("hydration_below_25.txt");
            for (RunnerLog log : dehydrated) {
                writer.println(log.toString());
            }
            writer.close();
        }

        if (gameState == STATE_SELECTION && key == ' ') {
            int currentTime = millis();
            keyPressTimes.add(currentTime);
            if (beatCount > 0) {
                int expectedTime = expectedBeats.get(beatCount - 1);
                int offset = abs(currentTime - expectedTime);
                if (offset < 100) {
                    score += 2;
                    feedback = "Perfect";
                } else if (offset < 250) {
                    score += 1;
                    feedback = "Good";
                } else {
                    feedback = "Miss";
                }
            }
        }
    }


    public void mousePressed() {
        if (gameState == STATE_FINISHED) {
            float btnX = width/2 - 50;
            float btnY = height/2 + 80;
            float btnW = 100, btnH = 40;
            if (mouseX >= btnX && mouseX <= btnX+btnW &&
                    mouseY >= btnY && mouseY <= btnY+btnH) {
                resetGame();
            }
        } else if (gameState == STATE_SELECTION_COMPLETE) {
            if (ui.isButtonClicked(mouseX, mouseY)) {
                gameState = STATE_RUNNING;
                gameStartTime = millis();
            }
        } else {
            ui.handleMousePressed(mouseX, mouseY);
        }
    }

    public void mouseDragged() {
        if (gameState != STATE_FINISHED) {
            ui.handleMouseDragged(mouseX);
        }
    }

    public void mouseReleased() {
        if (gameState != STATE_FINISHED) {
            ui.handleMouseReleased();
        }
    }

    public void updateRace() {
        if (ui.isDraggingSlider()) {
            speed = ui.getSpeed();
        }
        if (gameState == STATE_RUNNING && gameStartTime == 0) {
            gameStartTime = millis();
        }
        if (gameState == STATE_FINISHED) {
            return;
        }

        float baseSpeed = speed;
        float effectiveSpeed = baseSpeed;
        if (adrenalineModeActive) {
            effectiveSpeed = baseSpeed + adrenalineSpeedBonus;
            player.updateAdrenaline(baseSpeed);
        } else {
            player.update(baseSpeed);
        }
        ui.update(baseSpeed);

        backgroundX -= effectiveSpeed;
        if (backgroundX <= -backgroundImg.width) {
            backgroundX += backgroundImg.width;
        }
        groundTime += effectiveSpeed * 0.001f;
        speedKmh = effectiveSpeed * speedToKmh;
        speedKmh = min(speedKmh, 22f);
        distanceTravelled += (speedKmh / 3600.0f);
        updatePlayerPosition();

        // Collision check with aid stations.
        for (AidStation station : aidStations) {
            if (station.checkCollision(player)) {
                station.use(player);
                raceLog.add(new RunnerLog(frameCount, baseSpeed, player.stamina, player.hydration, "Used Aid Station"));
            }
        }

        // Trigger aid event (distance-based).
        float threshold = baseAidEventDistance * (22f / speedKmh);
        if (!aidEventActive && (distanceTravelled - lastAidEventDistance >= threshold)) {
            aidEventActive = true;
            aidEventStartTime = millis();
            aidEventHandled = false;
            aidEventDuration = max(1500, 3000 - baseSpeed * 50);
            aidStationStartX = width + 50;
            lastAidEventDistance = distanceTravelled;
        }
        if (aidEventActive && millis() - aidEventStartTime > aidEventDuration && !aidEventHandled) {
            feedback = "Aid: Miss!";
            aidEventActive = false;
            lastAidEventDistance = distanceTravelled;
        }
        if (player.hydration <= 0 && gameState == STATE_RUNNING) {
            gameState = STATE_CRAMPED;
            crampStartTime = millis();
            crampStartDistance = distanceTravelled;
            hopBoost = 0;
            feedback = "Cramps! Press UP to hop!";
        }

        // When in cramp mode, update the cramp hop physics.
        if (gameState == STATE_CRAMPED) {
            updateCrampMode();
        }

        // Trigger adrenaline mode via crowd events.
        if (!adrenalineModeActive && currentCrowd == null &&
                millis() - lastAdrenalineEventTime > nextAdrenalineEventInterval) {
            currentCrowd = new Crowd(this, width+50, baseY - 100, baseSpeed);
            crowdEventStartTime = millis();
        }
        if (currentCrowd != null) {
            currentCrowd.update();
            if (currentCrowd.isOffScreen()) {
                currentCrowd = null;
            }
            if (millis() - crowdEventStartTime > crowdEventDuration) {
                currentCrowd = null;
                adrenalineModeActive = true;
                adrenalineModeStartTime = millis();
                adrenalineSpeedBonus = 0;
                lastCheerTime = millis();
            }
        }
        if (adrenalineModeActive) {
            if (millis() - lastCheerTime > 500) {
                lastCheerTime = millis();
                currentCheer = cheers[(int) random(cheers.length)];
                cheerX = random(50, width-50);
                cheerY = random(50, height-50);
            }
            if (millis() - adrenalineModeStartTime > adrenalineModeDuration) {
                adrenalineModeActive = false;
                lastAdrenalineEventTime = millis();
                nextAdrenalineEventInterval = random(15000, 30000);
                adrenalineSpeedBonus = 0;
            }
        }

        // DNF condition.
        if (player.stamina <= 0 && distanceTravelled < raceDistance) {
            if (finalTimeSeconds == 0) {
                finalTimeSeconds = (millis()-gameStartTime)/1000.0f;
                finishDistance = distanceTravelled;
                averagePace = finishDistance>0 ? (finalTimeSeconds/60.0f)/finishDistance : 0;
                int totalSec = (int) finalTimeSeconds;
                int hrs = totalSec / 3600;
                int mins = (totalSec % 3600) / 60;
                int secs = totalSec % 60;

//  format the time as H:MM:SS
                finishTimeString = hrs + ":" +
                        (mins < 10 ? "0" + mins : mins) + ":" +
                        (secs < 10 ? "0" + secs : secs);
                println("DNF - Time: " + finishTimeString);
            }
            gameState = STATE_FINISHED;
            feedback = "DNF - You ran out of stamina!";
        }

        if (distanceTravelled >= 41.2f && distanceTravelled < 42.2f && player.stamina > 60 && gameState == STATE_RUNNING) {
            gameState = STATE_FINAL_KICK;
            adrenalineModeActive = true;
            adrenalineModeStartTime = millis();
            adrenalineSpeedBonus = 5; // Boost speed during final kick.
            feedback = "Final Kick! Push to the finish!";
        }

        if (distanceTravelled >= 42.2f && gameState != STATE_FINISHED) {
            gameState = STATE_FINISHED;
            finalTimeSeconds = (millis() - gameStartTime - totalPausedTime) / 1000.0f;
            finishDistance = distanceTravelled;
            averagePace = finishDistance > 0 ? (finalTimeSeconds / 60.0f) / finishDistance : 0;

            // Format the final time as a string
            int totalSec = (int) finalTimeSeconds*60;
            print("Finish Time: " + finalTimeSeconds + " seconds");
            int hrs = totalSec / 3600;
            int mins = (totalSec % 3600) / 60;
            int secs = totalSec % 60;

// format the time as H:MM:SS
            finishTimeString = hrs + ":" +
                    (mins < 10 ? "0" + mins : mins) + ":" +
                    (secs < 10 ? "0" + secs : secs);
            feedback = "Congratulations! You completed the marathon!";
        }

        // Check for entering cramp mode.
        if (gameState == STATE_RUNNING && player.stamina <= 0) {
            gameState = STATE_CRAMPED;
            crampStartTime = millis();
            crampStartDistance = distanceTravelled;
            hopBoost = 0;
            feedback = "Cramps! Press UP to hop!";
        }
    }

    void updatePlayerPosition() {
        float noiseVal = noise(groundTime + player.x * noiseMultiplier);
        float groundY = baseY - noiseVal * amplitude;
        player.y = groundY;
    }

    void updateCrampMode() {
        float groundLevel = baseY; // Ground level.

        if (inCrampHop) {
            // Update player's vertical position using simple physics.
            player.y -= crampHopVelocity;
            crampHopVelocity -= gravity;

            // Compute effective speed based on absolute hop velocity.
            float effectiveSpeed = abs(crampHopVelocity) * 0.8f; // Adjust multiplier as needed.
            backgroundX -= effectiveSpeed;
            distanceTravelled += effectiveSpeed * 0.005f; // Scale for distance progress.

            // Deplete hydration based on effective speed.
            player.hydration -= effectiveSpeed * 0.01f;
            if (player.hydration < 0) {
                player.hydration = 0;
            }

            // Check for landing.
            if (player.y >= groundLevel) {
                player.y = groundLevel;
                inCrampHop = false;
                crampHopVelocity = 0;
            }
            println("Cramp Mode: inHop = " + inCrampHop +
                    ", hopVelocity = " + crampHopVelocity +
                    ", player.y = " + player.y +
                    ", effectiveSpeed = " + effectiveSpeed);
        }

        // Trigger aid event based on distance, if not already active.
        if (!aidEventActive && (distanceTravelled - lastAidEventDistance >= baseAidEventDistance)) {
            aidEventActive = true;
            aidEventStartTime = millis();
        }

        // If 10 seconds pass in cramp mode, trigger DNF.
        if (millis() - crampStartTime >= 10000) {
            if (finalTimeSeconds == 0) {
                finalTimeSeconds = (millis() - gameStartTime) / 1000.0f;
                finishDistance = distanceTravelled;
                averagePace = finishDistance > 0 ? (finalTimeSeconds / 60.0f) / finishDistance : 0;
                int totalSec = (int) finalTimeSeconds;
                int hrs = totalSec / 3600;
                int mins = (totalSec % 3600) / 60;
                int secs = totalSec % 60;

//  format the time as H:MM:SS
                finishTimeString = hrs + ":" +
                        (mins < 10 ? "0" + mins : mins) + ":" +
                        (secs < 10 ? "0" + secs : secs);
            }
            gameState = STATE_FINISHED;
            feedback = "DNF - You ran out of stamina!";
        }
    }

    // --- Drawing Functions ---
    void drawMenu() {
        fill(0);
        textAlign(CENTER);
        textSize(32);
        text("RACE DAY: MARATHON MANAGER", width/2, height/2 - 20);
        textSize(20);
        text("Press 2 to Start Selection Challenge", width/2, height/2 + 20);
        text("Press 4 for How To Play", width/2, height/2 + 60);
    }

    void updateSelectionChallenge() {
        int currentTime = millis();
        if (beatCount < totalBeats && currentTime >= expectedBeats.get(beatCount)) {
            beatActive = true;
            lastBeatTime = currentTime;
            beatCount++;
        }
        if (beatActive && currentTime - lastBeatTime > 200) {
            beatActive = false;
            feedback = "Miss";
        }
        if (beatCount >= totalBeats && !beatActive) {
            endSelectionChallenge();
        }
    }

    void drawSelectionChallenge() {
        background(255);
        if (beatActive) {
            fill(0, 255, 0);
        } else {
            fill(200);
        }
        ellipse(width/2, height/2, 100, 100);
        fill(0);
        textAlign(CENTER);
        textSize(20);
        text("Feedback: " + feedback, width/2, height/2 + 100);
        text("Score: " + score, width/2, height/2 + 140);
        text("Press SPACE in sync with the beat!", width/2, height/2 - 100);
    }

    float getGroundHeight(float x) {
        float noiseVal = noise(groundTime + x * noiseMultiplier);
        return baseY - noiseVal * amplitude;
    }

    void drawRace() {
        drawBackground();
        drawGround();
        player.display();
        ui.drawBars(player.stamina, player.hydration, player.gutTolerance, player.gels);
        ui.drawSlider(speed);
        for (AidStation station : aidStations) {
            station.display();
        }
        if (currentCrowd != null) {
            currentCrowd.display();
        }
        if (aidEventActive) {
            pushStyle();
            drawAidEvent();
            popStyle();
        }
        if (adrenalineModeActive) {
            fill(255, 0, 0);
            textAlign(CENTER, TOP);
            textSize(28);
            text("ADRENALINE MODE!", width/2, 10);
            if (!currentCheer.equals("")) {
                fill(0, 0, 255);
                textSize(24);
                textAlign(CENTER, CENTER);
                text(currentCheer, cheerX, cheerY);
            }
        }
        fill(0);
        textAlign(LEFT, BOTTOM);
        textSize(16);
        text("Speed: " + nf(speedKmh, 1, 1) + " km/h", 10, height-140);
        text("Distance: " + nf(distanceTravelled, 1, 2) + " km / " + raceDistance + " km", 10, height-120);
        float delta = 100.0f;
        float leftHeight = getGroundHeight(player.x - delta*0.5f);
        float rightHeight = getGroundHeight(player.x + delta*0.5f);
        float slope = (leftHeight - rightHeight) / delta;
        float angleDegrees = degrees(atan(slope));
        float gradePercent = slope * 100;
        text("Slope: " + nf(slope, 1, 2), 10, height-40);
        text("Angle: " + nf(angleDegrees, 1, 1) + "°", 10, height-25);
        text("Grade: " + nf(gradePercent, 1, 1) + "%", 10, height-10);
        if (gameStartTime != 0) {
            float timeScale = 62.0f;
            float realTimeSeconds = (millis()-gameStartTime-totalPausedTime) / 1000.0f;
            float scaledTimeSeconds = realTimeSeconds*timeScale;
            int totalSec = (int) scaledTimeSeconds;
            int hrs = totalSec/3600;
            int mins = (totalSec%3600)/60;
            int secs = totalSec%60;
            String timeString = hrs + ":" + (mins < 10 ? "0"+mins:mins) + ":" + (secs < 10 ? "0"+secs:secs);
            fill(0);
            textAlign(RIGHT, TOP);
            textSize(16);
            text("Time: " + timeString, width-10, 10);
        }
    }

    void drawBackground() {
        image(backgroundImg, backgroundX, 0, backgroundImg.width, height);
        image(backgroundImg, backgroundX+backgroundImg.width, 0, backgroundImg.width, height);
    }

    void drawGround() {
        stroke(0);
        fill(150, 150, 150);
        beginShape();
        vertex(0, height);
        for (int x = 0; x <= width; x+=groundResolution) {
            float noiseVal = noise(groundTime + x * noiseMultiplier);
            float y = baseY - noiseVal * amplitude;
            vertex(x, y);
        }
        vertex(width, height);
        endShape(CLOSE);
    }

    void drawAidEvent() {
        pushStyle();
        float rawProgress = (millis()-aidEventStartTime) / (float) aidEventDuration;
        float progress = rawProgress % 1.0f;
        float currentSquareX = lerp(aidStationStartX, player.x, progress);
        float squareY = player.y;
        if (aidEventImg != null) {
            image(aidEventImg, currentSquareX-50, squareY-125, 200, 200);
        } else {
            fill(0, 255, 0);
            rectMode(CENTER);
            rect(currentSquareX, squareY, 40, 40);
        }
        float currentRingRadius = lerp(aidOuterCircleStartRadius, aidInnerCircleRadius, progress);
        float centerX = width/2;
        float centerY = height/2;
        noFill();
        stroke(0);
        ellipse(centerX, centerY, currentRingRadius*2, currentRingRadius*2);
        fill(200);
        noStroke();
        ellipse(centerX, centerY, aidInnerCircleRadius*2, aidInnerCircleRadius*2);
        fill(0);
        textAlign(CENTER, CENTER);
        textSize(16);
        text("PRESS SPACE", centerX, centerY);
        popStyle();
    }

    void drawFinishScreen() {
        background(255);
        fill(0);
        textAlign(CENTER, CENTER);
        textSize(32);
        if (distanceTravelled >= raceDistance) {
            if (parseFinishTimeToSeconds(finishTimeString) < 7260) { // Check for world record
                textSize(24);
                fill(255, 0, 0); // Red color for emphasis
                text("New World Record!", width / 2, height / 2 + 40);
            }
            text("Congratulations! You finished the marathon!", width / 2, height / 2 - 100);
        } else {
            text("DNF - You ran out of stamina!", width / 2, height / 2 - 100);
        }
        textSize(20);
        text("Time: " + finishTimeString, width / 2, height / 2 - 60); // Display final time
        text("Distance: " + nf(finishDistance, 1, 2) + " km", width / 2, height / 2 - 30);
        //text("Avg Pace: " + nf(averagePace, 1, 2) + " min/km", width / 2, height / 2);
        rectMode(CORNER);
        fill(200);
        stroke(0);
        float btnX = width / 2 - 50;
        float btnY = height / 2 + 80;
        float btnW = 100, btnH = 40;
        rect(btnX, btnY, btnW, btnH, 5);
        fill(0);
        textSize(18);
        textAlign(CENTER, CENTER);
        text("Try Again", width / 2, btnY + btnH / 2);
    }
    private int parseFinishTimeToSeconds(String finishTimeString) {
        String[] parts = finishTimeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    void resetGame() {
        gameState = STATE_MENU;
        player = new PlayerRunner(this);
        ui = new UIManager(this);
        aidStations.clear();
        raceLog.clear();
        expectedBeats.clear();
        keyPressTimes.clear();
        beatCount = 0;
        score = 0;
        feedback = "";
        groundTime = 0;
        backgroundX = 0;
        distanceTravelled = 0;
        gameStartTime = 0;
        finalTimeSeconds = 0;
        lastAidEventDistance = 0;
        aidEventActive = false;
        adrenalineModeActive = false;
        adrenalineSpeedBonus = 0;
        currentCrowd = null;
    }

    void startSelectionChallenge() {
        expectedBeats.clear();
        keyPressTimes.clear();
        score = 0;
        beatCount = 0;
        lastBeatTime = millis();
        feedback = "";
        for (int i = 0; i < totalBeats; i++) {
            expectedBeats.add(lastBeatTime + i * beatInterval);
        }
    }

    void endSelectionChallenge() {
        String runnerClass;
        if (score >= 14) {
            runnerClass = "Elite Runner";
            player.stamina = 120;
            player.hydration = 120;
        } else if (score >= 8) {
            runnerClass = "Balanced Runner";
            player.stamina = 100;
            player.hydration = 100;
        } else {
            runnerClass = "Novice Runner";
            player.stamina = 80;
            player.hydration = 80;
        }
        player.setPlayerClass(runnerClass);
        gameState = STATE_SELECTION_COMPLETE;
    }

    void drawSelectionCompleteScreen() {
        background(255);
        fill(0);
        textAlign(CENTER, CENTER);
        textSize(32);
        text("Challenge Complete!", width/2, height/2 - 80);
        textSize(24);
        text("Score: " + score, width/2, height/2 - 40);
        text("You unlocked: " + player.playerClass, width/2, height/2);
        float btnX = width/2 - 75;
        float btnY = height/2 + 40;
        float btnW = 150, btnH = 50;
        rectMode(CORNER);
        fill(200);
        stroke(0);
        rect(btnX, btnY, btnW, btnH, 5);
        fill(0);
        textSize(18);
        textAlign(CENTER, CENTER);
        text("Start Game", width/2, btnY+btnH/2);
        ui.setButtonBounds(btnX, btnY, btnW, btnH);
    }

    void drawPauseScreen() {
        fill(0,150);
        rect(0,0,width,height);
        fill(255);
        textAlign(CENTER, CENTER);
        textSize(32);
        text("Game Paused", width/2, height/2 - 20);
        textSize(20);
        text("Press 'P' to Resume", width/2, height/2 + 20);
    }

    void exportLogToFile(String fileName) {
        PrintWriter writer = createWriter(fileName);
        for (RunnerLog log : raceLog) {
            writer.println(log.toString());
        }
        writer.close();
    }

    void sortLogsBySpeed() {
        Collections.sort(raceLog, new Comparator<RunnerLog>() {
            public int compare(RunnerLog a, RunnerLog b) {
                return Float.compare(b.speed, a.speed);
            }
        });
    }

    ArrayList<RunnerLog> searchLogsByHydration(float threshold) {
        ArrayList<RunnerLog> results = new ArrayList<>();
        for (RunnerLog log : raceLog) {
            if (log.hydration < threshold) {
                results.add(log);
            }
        }
        return results;
    }
}
