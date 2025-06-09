import processing.core.PApplet;

public class UIManager {

    PApplet p;
    boolean draggingSlider = false;
    float sliderMin = 100, sliderMax = 300;
    float sliderX = sliderMin;
    float buttonX, buttonY, buttonWidth, buttonHeight;


    public UIManager(PApplet p) {
        this.p = p;
    }

    public void update(float speed) {
        sliderX = p.map(speed, 1, 10, sliderMin, sliderMax);
    }

    public void drawBars(float stamina, float hydration, float gutTolerance, int gels) {
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(12);
        drawBar(10, 10, stamina * 2, 20, p.color(0, 255, 0), "Stamina");
        drawBar(10, 40, hydration * 2, 20, p.color(0, 0, 255), "Hydration");
        drawBar(10, 70, gutTolerance * 2, 20, p.color(255, 165, 0), "Fuel");
        p.fill(0);
        p.text("Gels: " + gels, 10, 110);
    }

    private void drawBar(float x, float y, float widthBar, float heightBar, int barColor, String label) {
        p.fill(barColor);
        p.rect(x, y, widthBar, heightBar);
        p.fill(0);
        p.text(label, x, y + heightBar / 2);
    }

    public void drawSlider(float speed) {
        p.fill(200);
        p.rect(sliderMin, 550, sliderMax - sliderMin, 20);
        p.fill(0);
        p.ellipse(sliderX, 560, 20, 20);
    }

    public boolean isDraggingSlider() {
        return draggingSlider;
    }

    public void handleMousePressed(float mouseX, float mouseY) {
        if (p.dist(mouseX, mouseY, sliderX, 560) < 10) {
            draggingSlider = true;
        }
    }

    public void handleMouseDragged(float mouseX) {
        if (draggingSlider) {
            sliderX = p.constrain(mouseX, sliderMin, sliderMax);
        }
    }

    public void handleMouseReleased() {
        draggingSlider = false;
    }
    public void setButtonBounds(float x, float y, float width, float height) {
        this.buttonX = x;
        this.buttonY = y;
        this.buttonWidth = width;
        this.buttonHeight = height;
    }

    public boolean isButtonClicked(float mouseX, float mouseY) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    public float getSpeed() {
        return p.map(sliderX, sliderMin, sliderMax, 1, 10);
    }
}
