public class AlarmFunctional {
    final int IN_ALARM_DISPLAY = 1;
    final int IN_ALARMS = 1;
    final int IN_CHECK = 1;
    final int IN_DISABLED = 1;
    final int IN_MONITOR = 2;
    final int IN_NOT_ON = 2;
    final int IN_NO_ACTIVE_CHILD = 0;
    final int IN_NO = 1;
    final int IN_OFF = 2;
    final int IN_OFF_I = 1;
    final int IN_ON = 3;
    final int IN_ON_A = 2;
    final int IN_SILENCED = 4;
    final int IN_YES = 2;
    final int IN_YES_O = 3;
    final int IN_COUNTING = 3;

    int checkOverInfusionFlowRate(B localB) {
        int ov;

        ov = 0;
        /*
        if (localB.inTherapy) {
            //int div1 = Divs32.divS32(localB.toleranceMax, 100);
            //int div2 = Divs32.divS32(localB.toleranceMin, 100);

            int commandedFlowRate = localB.commandedFlowRate;
            int flowRate = localB.flowRate;

            if (localB.flowRate > localB.flowRateHigh) {
                ov = 1;
            } else if (flowRate > commandedFlowRate * 1 + commandedFlowRate) {
                ov = 1;
            } else {
                if (flowRate > commandedFlowRate * 1 + commandedFlowRate) {
                    ov = 2;
                }
            }
        }
        */
        if (localB.inTherapy) {
            int commandedFlowRate = 2;
            if (localB.flowRate > localB.flowRateHigh) {
                ov = 1;
            } else {
                ov = 2;
            }
        }


        // ov stands for OverInfusion
        return ov;
    }

    public static void main(String[] args) {
        AlarmFunctional af = new AlarmFunctional();
        B b = new B();
        af.checkOverInfusionFlowRate(b);
    }
}