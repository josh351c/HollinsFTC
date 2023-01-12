package org.firstinspires.ftc.teamcode;

import com.outoftheboxrobotics.photoncore.PhotonCore;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name="teleoppowerplay2", group="Iterative Opmode")
public class teleoppowerplay2 extends OpMode {

    public enum LiftState {
        LIFT_GRABNEW,
        LIFT_CLAWCLOSE,
        LIFT_DROPCONE,
        LIFT_DROPCONEMEDIUM,
        LIFT_EXTENDSLIDE,
        LIFT_RETRACTSLIDE,
        LIFT_CLAWOPEN,
        LIFT_MANUAL_CONTROL
    }

    ElapsedTime liftTimer = new ElapsedTime();

    LiftState liftState = LiftState.LIFT_GRABNEW;

    private DcMotor front_left  = null;
    private DcMotor front_right = null;
    private DcMotor back_left   = null;
    private DcMotor back_right  = null;
    private static DcMotor tilt_arm;
    private static DcMotor slide_extension;
    private DcMotor rotate_arm;

    public int rotate_collect = 1122;
    public int tilt_collect = 655;
    public int slide_collect = 1155;
    public int rotate_drop = 235;
    public int tilt_drop = -2316;
    public int slide_drop = 1360;
    public int slide_var = 0;
    public double CLAW_HOLD = 0.35;
    public double CLAW_DEPOSIT = 0.7;
    final double CLAWTILT_COLLECT = 0.62;
    final double CLAWTILT_DEPOSIT = 0.72;

    double odometry_forward_static = 0.5;
    double odometry_strafe_static = 0.5;

    public int tilt_ticks;
    public int extension_ticks;
    public double changing_tilt_ticks = 0;
    public int rotation_ticks = 0;
    private Servo claw = null;
    private Servo tilt_claw = null;
    private Servo odometry_forward = null;
    private Servo odometry_strafe = null;
    int MinPositionTicks = 0;

    boolean slidevar = true;

    int tilt_position = 1;
    int slide_position = 0;

    double tiltclaw_4 = 0.9;
    double tiltclaw_3 = 0.78;
    double tiltclaw_2 = 0.7;
    double tiltclaw_0 = 0.65;

    Gamepad currentGamepad1;
    Gamepad previousGamepad1;

    @Override
    public void init() {

        PhotonCore.enable();

        front_left   = hardwareMap.get(DcMotor.class, "front_left");
        front_right  = hardwareMap.get(DcMotor.class, "front_right");
        back_left    = hardwareMap.get(DcMotor.class, "back_left");
        back_right   = hardwareMap.get(DcMotor.class, "back_right");
        slide_extension  = hardwareMap.get(DcMotor.class,"slide_extension");
        tilt_arm = hardwareMap.get(DcMotor.class,"tilt_arm");
        claw = hardwareMap.get(Servo.class,"claw");
        tilt_claw = hardwareMap.get(Servo.class,"tilt_claw");
        rotate_arm = hardwareMap.get(DcMotor.class,"rotate_arm");
        odometry_forward = hardwareMap.get(Servo.class, "odometry_forward");
        odometry_strafe = hardwareMap.get(Servo.class, "odometry_strafe");
        slide_extension.setDirection(DcMotor.Direction.REVERSE);

        slide_extension.setTargetPosition(MinPositionTicks);
        tilt_arm.setTargetPosition(MinPositionTicks);
        rotate_arm.setTargetPosition(MinPositionTicks);
        slide_extension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        tilt_arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rotate_arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slide_extension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        tilt_arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rotate_arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        front_left.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        front_right.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        back_left.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        back_right.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        //claw.setPosition(CLAW_DEPOSIT);
        //tilt_claw.setPosition(CLAWTILT_COLLECT);
        odometry_forward.setPosition(0.55);
        odometry_strafe.setPosition(0.2);
        //claw         = hardwareMap.get(Servo.class,"claw");
        front_right.setDirection(DcMotor.Direction.REVERSE);
        back_right.setDirection(DcMotor.Direction.REVERSE);

        PhotonCore.enable();

        currentGamepad1 = new Gamepad();
        previousGamepad1 = new Gamepad();
    }

    @Override
    public void loop() {

        try {
            // Store the gamepad values from the previous loop iteration in
            // previousGamepad1/2 to be used in this loop iteration.
            // This is equivalent to doing this at the end of the previous
            // loop iteration, as it will run in the same order except for
            // the first/last iteration of the loop.
            previousGamepad1.copy(currentGamepad1);

            // Store the gamepad values from this loop iteration in
            // currentGamepad1/2 to be used for the entirety of this loop iteration.
            // This prevents the gamepad values from changing between being
            // used and stored in previousGamepad1/2.
            currentGamepad1.copy(gamepad1);
        }
        catch (RobotCoreException e) {
            // Swallow the possible exception, it should not happen as
            // currentGamepad1/2 are being copied from valid Gamepads.
        }


        tilt_ticks = tilt_arm.getCurrentPosition();
        extension_ticks = slide_extension.getCurrentPosition();
        rotation_ticks = rotate_arm.getCurrentPosition();
        changing_tilt_ticks = changing_tilt_ticks + (0.5*(-gamepad2.right_stick_y));
        telemetry.addData("changing tilt ticks:",changing_tilt_ticks);
        telemetry.addData("changing rotation ticks", rotation_ticks);
        telemetry.addData("current state", liftState);
        telemetry.addData("claw position", claw.getPosition());
        telemetry.addData("lifttimer", liftTimer.seconds());
        telemetry.addData("stuff", Math.abs(slide_extension.getCurrentPosition() -  slide_collect));
        telemetry.addData("odometry_forward", odometry_forward.getPosition());
        telemetry.addData("odometry_strafe", odometry_strafe.getPosition());
        rotate_arm.setPower(1);
        tilt_arm.setPower(1);
        slide_extension.setPower(1);

        if (gamepad1.back){
            liftState = LiftState.LIFT_GRABNEW;
        }
        else if (gamepad1.right_bumper && gamepad1.y){
            // High Pole Right Teleop
            rotate_drop = 240;
            slide_drop = 1360;
            tilt_drop = 2316;
        }
        else if (gamepad1.right_bumper && gamepad1.b){
            // Medium Pole Right Teleop
            rotate_drop = -312;
            slide_drop = 563;
            tilt_drop = -1383;
        }
        else if (gamepad1.right_bumper && gamepad1.a){
            // Low Pole Right Teleop
            rotate_drop = -966;
            slide_drop = 0;
            tilt_drop = -826; 
        }

        switch (liftState) {
            case LIFT_GRABNEW:
                tilt_claw.setPosition( CLAWTILT_COLLECT);
                 slide_var = 0;
                if (gamepad1.a){
                    rotate_arm.setTargetPosition( rotate_collect);
                    tilt_arm.setTargetPosition( tilt_collect);
                        if (Math.abs(rotate_arm.getCurrentPosition() -  rotate_collect) <= 20){
                            slide_extension.setTargetPosition( slide_collect);
                            if (Math.abs(slide_extension.getCurrentPosition() -  slide_collect) <= 8 && gamepad1.x) {
                                claw.setPosition( CLAW_HOLD);
                                liftTimer.reset();
                                liftState = LiftState.LIFT_CLAWCLOSE;

                            }
                        }
                }
                break;

            case LIFT_MANUAL_CONTROL:
                MinPositionTicks += (-gamepad1.right_stick_x*10);

                if (gamepad1.left_bumper){
                    claw.setPosition(0.7);
                }
                else if (gamepad1.right_bumper){
                    claw.setPosition(0.35);
                }

                if (currentGamepad1.b && !previousGamepad1.b) {
                    MinPositionTicks = MinPositionTicks - 311;
                }
                else if (currentGamepad1.x && !previousGamepad1.x){
                    MinPositionTicks = MinPositionTicks + 311;
                }


                if (currentGamepad1.dpad_up && !previousGamepad1.dpad_up) {
                    tilt_position = tilt_position + 1;
                }
                else if (currentGamepad1.dpad_down && !previousGamepad1.dpad_down){
                    tilt_position = tilt_position - 1;
                }

                if (currentGamepad1.y && !previousGamepad1.y) {
                    slidevar = true;
                    slide_position = slide_position + 1;
                }
                else if (currentGamepad1.a && !previousGamepad1.a){
                    slidevar = true;
                    slide_position = slide_position - 1;
                }

                if (tilt_position == 0){
                    tilt_arm.setTargetPosition(900);
                    tilt_claw.setPosition(0.60);
                }
                else if (tilt_position == 1){
                    tilt_arm.setTargetPosition(0);

                }
                else if (tilt_position == 2){
                    tilt_arm.setTargetPosition(-750);
                    tilt_claw.setPosition(tiltclaw_2);
                    if (gamepad1.right_trigger>0.5){
                        tilt_claw.setPosition(tiltclaw_2+0.4);
                    }
                }
                else if (tilt_position == 3){
                    tilt_arm.setTargetPosition(-1400);
                    tilt_claw.setPosition(tiltclaw_3);
                    if (gamepad1.right_trigger>0.5){
                        tilt_claw.setPosition(tiltclaw_3+0.4);
                    }

                }
                else if (tilt_position == 4){
                    tilt_arm.setTargetPosition(-2650);
                    tilt_claw.setPosition(tiltclaw_4);
                    if (gamepad1.right_trigger>0.5){
                        tilt_claw.setPosition(tiltclaw_4+0.4);
                    }
                }
                else if (tilt_position == 5){
                    tilt_arm.setTargetPosition(-3000);
                    tilt_claw.setPosition(tiltclaw_4);
                    if (gamepad1.right_trigger>0.5){
                        tilt_claw.setPosition(tiltclaw_4+0.4);
                    }
                }

                if (gamepad1.dpad_right){
                    tilt_position = 4;
                }
                else if (gamepad1.dpad_left){
                    tilt_position = 0;
                }

                if (slide_position == 0 && slidevar){
                    slide_extension.setTargetPosition(0);
                }
                else if (slide_position == 1 && slidevar){
                    slide_extension.setTargetPosition(550);
                }
                else if (slide_position == 2 && slidevar){
                    slide_extension.setTargetPosition(987);
                }
                else if (slide_position == 3 && slidevar){
                    slide_extension.setTargetPosition(1480);
                }
                if (-gamepad1.right_stick_y < -0.75){
                    slidevar = false;
                    slide_extension.setTargetPosition(0);
                }
                else if(-gamepad1.right_stick_y > 0.75){
                    slidevar = false;
                    slide_extension.setTargetPosition(1480);
                }


                rotate_arm.setTargetPosition(MinPositionTicks);


                break;
            case LIFT_CLAWCLOSE:
                if (liftTimer.seconds() >= 0.4) {
                    liftState = LiftState.LIFT_DROPCONE;
                }
                break;

            case LIFT_DROPCONE:
                slide_extension.setTargetPosition(0);
                if (slide_extension.getCurrentPosition() <= 100) {
                    tilt_arm.setTargetPosition( tilt_drop);
                    rotate_arm.setTargetPosition( rotate_drop);
                    tilt_claw.setPosition( CLAWTILT_DEPOSIT);
                    if (Math.abs(tilt_arm.getCurrentPosition() -  tilt_drop) <= 8 && Math.abs(rotate_arm.getCurrentPosition() -  rotate_drop) <= 10) {
                        liftState = LiftState.LIFT_EXTENDSLIDE;
                    }
                }
                    break;
            case LIFT_EXTENDSLIDE:
                slide_extension.setTargetPosition( slide_drop);
                if ((slide_extension.getCurrentPosition() >= ( slide_drop - 100)) && gamepad1.b) {
                    tilt_claw.setPosition(( CLAWTILT_DEPOSIT + 0.3));

                    if (gamepad1.right_trigger > 0.5) {
                        claw.setPosition( CLAW_DEPOSIT);
                        liftState = LiftState.LIFT_RETRACTSLIDE;
                    }
                }
                else {
                    tilt_claw.setPosition( CLAWTILT_DEPOSIT);
                    }
                break;
            case LIFT_RETRACTSLIDE:
                slide_extension.setTargetPosition(0);
                if (slide_extension.getCurrentPosition() <= 400){
                    liftState = LiftState.LIFT_GRABNEW;
                }
                break;
        }





        //power = (power + (power-(gamepad2.right_stick_y))/10)*gamepad2.right_stick_y;
        //power = ((1/(gamepad2.right_stick_y))*0.08)*gamepad2.right_stick_y;
        //power = ((gamepad2.right_stick_y)+(gamepad2.right_stick_y/Math.abs(gamepad2.right_stick_y))*Math.abs((0.5)*gamepad2.right_stick_y-power));

/*        if (gamepad2.y) {
            slide_extension.setPower(1);
            slide_extension.setTargetPosition(MaxPositionTicks);
        }
        else if (gamepad2.a) {
            slide_extension.setPower(1);
            slide_extension.setTargetPosition(MinPositionTicks);
        }
        else {
        }*/

/*        if (gamepad2.dpad_right){
            tilt_arm.setPower(1);
            changing_tilt_ticks = 30;
        }
        else if (gamepad2.dpad_left){
            tilt_arm.setPower(1);
            changing_tilt_ticks = 250;
        }
        else if (gamepad2.dpad_down){
            tilt_arm.setPower(1);
            changing_tilt_ticks = 280;
        }
        else{

        }*/

        //claw.setPower(gamepad2.right_trigger-gamepad2.left_trigger);

        /*if (gamepad2.right_bumper)
            claw.setPosition(1);
        else if (gamepad2.left_bumper)
            claw.setPosition(0);
        else
            claw.setPosition(0);

         */

        telemetry.addData("encoder ticks for slide",extension_ticks);
        telemetry.addData("encoder ticks for tilt",tilt_ticks);

        //slide_extension.setPower(gamepad2.left_stick_y);

        double drive  = gamepad1.left_stick_y;
        double strafe = -gamepad1.left_stick_x;
        double twist  = -gamepad1.right_stick_x;

        double[] speeds = {
                (drive + strafe + twist),
                (drive - strafe - twist),
                (drive - strafe + twist),
                (drive + strafe - twist)
        };
        double max = Math.abs(speeds[0]);
        for (int i = 0; i < speeds.length; i++) {
            if ( max < Math.abs(speeds[i]) ) max = Math.abs(speeds[i]);
        }
        if (max > 1) {
            for (int i = 0; i < speeds.length; i++) speeds[i] /= max;
        }

        front_left.setPower(speeds[0]);
        front_right.setPower(speeds[1]);
        back_left.setPower(speeds[2]);
        back_right.setPower(speeds[3]);
    }
}

