public class RunnerLog {
    int timeStamp;
    float speed, stamina, hydration;
    String eventDescription;

    public RunnerLog(int timeStamp, float speed, float stamina, float hydration, String eventDescription) {
        this.timeStamp = timeStamp;
        this.speed = speed;
        this.stamina = stamina;
        this.hydration = hydration;
        this.eventDescription = eventDescription;
    }

    @Override
    public String toString() {
        return timeStamp + ", " + speed + ", " + stamina + ", " + hydration + ", " + eventDescription;
    }


}
