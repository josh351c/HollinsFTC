package org.firstinspires.ftc.teamcode.auton;

import static org.firstinspires.ftc.teamcode.drive.DriveConstants.variable_slide_ticks;
import static org.firstinspires.ftc.teamcode.drive.DriveConstants.variable_tilt_ticks;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.outoftheboxrobotics.photoncore.PhotonCore;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.openftc.apriltag.AprilTagDetection;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

import java.util.ArrayList;


// adb connect 192.168.43.1:5555

@Autonomous(name="FSM AUTO HIGH POLE SPEED")
public class FSMAutoHighPoleSpeed extends OpMode {

    /*public void init_loop(){
        {
            ArrayList<AprilTagDetection> currentDetections = aprilTagDetectionPipeline.getLatestDetections();

            if(currentDetections.size() != 0)
            {
                boolean tagFound = false;

                for(AprilTagDetection tag : currentDetections)
                {
                    if(tag.id == LEFT || tag.id == MIDDLE || tag.id == RIGHT)
                    {
                        tagOfInterest = tag;
                        // Here we set the integer we KNOW is is one of the three from the test above
                        parkingTag = tag.id;
                        tagFound = true;
                        break;
                    }
                }

                if(tagFound)
                {
                    telemetry.addLine("Tag of interest is in sight!\n\nLocation data:");
                    tagToTelemetry(tagOfInterest);
                }
                else
                {
                    telemetry.addLine("Don't see tag of interest :(");

                    if(tagOfInterest == null)
                    {
                        telemetry.addLine("(The tag has never been seen)");
                    }
                    else
                    {
                        telemetry.addLine("\nBut we HAVE seen the tag before; last seen at:");
                        tagToTelemetry(tagOfInterest);
                    }
                }

            }
            else
            {
                telemetry.addLine("Don't see tag of interest :(");

                if(tagOfInterest == null)
                {
                    telemetry.addLine("(The tag has never been seen)");
                }
                else
                {
                    telemetry.addLine("\nBut we HAVE seen the tag before; last seen at:");
                    tagToTelemetry(tagOfInterest);
                }

            }

            telemetry.update();
        }

        if(tagOfInterest != null)
        {
            telemetry.addLine("Tag snapshot:\n");
            tagToTelemetry(tagOfInterest);
            telemetry.update();
        }
        else
        {
            telemetry.addLine("No tag snapshot available, it was never sighted during the init loop :(");
            telemetry.update();
        }
    }*/

    public enum LiftState {
        LIFT_STARTDROP,
        LIFT_GETNEW,
        LIFT_RETRACTSLIDE,
        LIFT_HOLD,
        LIFT_LETGO,
        LIFT_DROPCYCLE,
        LIFT_INC,
        PARKING_STATE,
        LIFT_WAITSTATE,
        FINISH
    }


    // The liftState variable is declared out here
    // so its value persists between loop() calls
    LiftState liftState = LiftState.LIFT_STARTDROP;

    /*OpenCvCamera camera;
    AprilTagDetectionPipeline aprilTagDetectionPipeline;
*/
    static final double FEET_PER_METER = 3.28084;

    // Lens intrinsics
    // UNITS ARE PIXELS
    // NOTE: this calibration is for the C920 webcam at 800x448.
    // You will need to do your own calibration for other configurations!
    double fx = 578.272;
    double fy = 578.272;
    double cx = 402.145;
    double cy = 221.506;

    // UNITS ARE METERS
    double tagsize = 0.166;

    // Tag ID 1,2,3 from the 36h11 family
    int LEFT = 1;
    int MIDDLE = 2;
    int RIGHT = 3;
/*
    // This Integer will be set to a default LEFT if no tag is found
    int parkingTag = LEFT;

    AprilTagDetection tagOfInterest = null;*/

    TrajectorySequence BlueOnRedGoMiddle;
    TrajectorySequence BlueOnRedGoRight;
    TrajectorySequence BlueOnRedGoLeft;

    public DcMotorEx slide_extension;
    public DcMotorEx tilt_arm;
    public DcMotorEx rotate_arm;
    public Servo claw;
    public Servo tilt_claw;

    ElapsedTime liftTimer = new ElapsedTime();
    ElapsedTime parkingTimer = new ElapsedTime();

    SampleMecanumDrive drive;

    int cones_dropped = 0;
    int CONES_DESIRED = 4;

    boolean switchvar = true;

    final double CLAW_HOLD = 0; // the idle position for the dump servo
    final double CLAW_DEPOSIT = 0.25; // the dumping position for the dump servo

    final double CLAWTILT_END = 0.3;
    final double CLAWTILT_COLLECT = 0.6;
    final double CLAWTILT_DEPOSIT = 0.8;

    // the amount of time the dump servo takes to activate in seconds
    final double DUMP_TIME = 1;
    final double ROTATE_TIME = 0.3; // the amount of time it takes to rotate 135 degrees
    final double EXTENSION_TIME = 0.6; // e amount of time it takes to extend from 0 to 2250 on the slide

    final int SLIDE_LOW = 1000; // the low encoder position for the lift
    final int SLIDE_COLLECT = 1131; // the high encoder position for the lift
    final int SLIDE_DROPOFF = 1546;

    // TODO: find encoder values for tilt
    int TILT_LOW = 0;
    final int TILT_HIGH = 600;

    // TODO: find encoder values for rotation
    final int ROTATE_COLLECT = 258;
    final int ROTATE_DROP = -782;

    //public TrajectorySequence VariablePath;

    public void init() {
        liftTimer.reset();
        PhotonCore.enable();

        drive = new SampleMecanumDrive(hardwareMap);

        drive.setPoseEstimate(new Pose2d(36, 66, Math.toRadians(270)));

        slide_extension = hardwareMap.get(DcMotorEx.class,"slide_extension");
        tilt_arm = hardwareMap.get(DcMotorEx.class,"tilt_arm");
        rotate_arm = hardwareMap.get(DcMotorEx.class,"rotate_arm");
        claw = hardwareMap.get(Servo.class,"claw");
        tilt_claw = hardwareMap.get(Servo.class,"tilt_claw");

        //rotate_arm = hardwareMap.get(DcMotorEx.class,"rotate_arm");

        slide_extension.setDirection(DcMotor.Direction.REVERSE);
        slide_extension.setTargetPosition(variable_slide_ticks);
        slide_extension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slide_extension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        tilt_arm.setTargetPosition(variable_tilt_ticks);
        tilt_arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        tilt_arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rotate_arm.setTargetPosition(0);
        rotate_arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rotate_arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        claw.setPosition(CLAW_HOLD);
        tilt_claw.setPosition(0.3);


/*
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);
        aprilTagDetectionPipeline = new AprilTagDetectionPipeline(tagsize, fx, fy, cx, cy);

        camera.setPipeline(aprilTagDetectionPipeline);
        camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener()
        {
            @Override
            public void onOpened()
            {
                camera.startStreaming(800,448, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode)
            {

            }
        });


        telemetry.setMsTransmissionInterval(50);
*/

        //init_loop();{
        // }
        //while (tagOfInterest == null)


        BlueOnRedGoMiddle = drive.trajectorySequenceBuilder(new Pose2d(42,14, Math.toRadians(270)))
                .strafeRight(4)
                .back(24)
                .build();
        BlueOnRedGoRight = drive.trajectorySequenceBuilder(new Pose2d(42,13.5, Math.toRadians(270)))
                .strafeRight(22)
                .back(24)
                //.lineToConstantHeading(new Vector2d(14,14))
                //.splineToLinearHeading(new Pose2d(14,36, Math.toRadians(270)), Math.toRadians(270))
                .build();
        BlueOnRedGoLeft = drive.trajectorySequenceBuilder(new Pose2d(42,13.5, Math.toRadians(270)))
                .strafeLeft(14)
                .back(24)
                //.lineToConstantHeading(new Vector2d(54,14))
                //.splineToLinearHeading(new Pose2d(60,36, Math.toRadians(270)), Math.toRadians(270))
                .build();

        TrajectorySequence BlueOnRedGoCycle = drive.trajectorySequenceBuilder(new Pose2d(36, 66, Math.toRadians(270)))
                .lineTo(new Vector2d(36,60))
                .addDisplacementMarker(() -> switchvar = true)
                .lineTo(new Vector2d(36,24))
                .splineToConstantHeading(new Vector2d(41.3,12.2), Math.toRadians(270))
                .build();
        //init_loop();
        //drive.followTrajectorySequenceAsync(BlueOnRedGoCycle);
        tilt_claw.setPosition(CLAWTILT_END);
        claw.setPosition(CLAW_HOLD);

    }

    public void loop() {

        Pose2d poseEstimate = drive.getPoseEstimate();

        telemetry.addData("x", poseEstimate.getX());
        telemetry.addData("y", poseEstimate.getY());
        telemetry.addData("heading", poseEstimate.getHeading());
        telemetry.addData("encoder ticks for slide",slide_extension.getCurrentPosition());
        telemetry.addData("encoder ticks for tilt",tilt_arm.getCurrentPosition());
        telemetry.addData("rotation ticks", rotate_arm.getCurrentPosition());
        telemetry.addData("claw position", claw.getPosition());
        telemetry.addData("claw tilt", tilt_claw.getPosition());
        telemetry.addData("timer",liftTimer.seconds());
        telemetry.addData("liftstate", liftState);
        telemetry.addData("cones dropped", cones_dropped);
        //telemetry.addData("tag location", tagOfInterest.id);
        telemetry.addData("drive", drive.isBusy());

        tilt_arm.setPower(1);
        rotate_arm.setPower(1);
        slide_extension.setPower(1);


        //telemetry.update();

        switch (liftState) {
            case LIFT_STARTDROP:
                tilt_claw.setPosition(0.9);
                rotate_arm.setPower(1);
                tilt_arm.setTargetPosition(TILT_HIGH);
                rotate_arm.setTargetPosition(ROTATE_DROP);
                if ((Math.abs(rotate_arm.getCurrentPosition() - ROTATE_DROP) <= 30) && (Math.abs(tilt_arm.getCurrentPosition() - TILT_HIGH)) <= 5) {
                    slide_extension.setTargetPosition(SLIDE_DROPOFF);
                    if ((Math.abs(slide_extension.getCurrentPosition() - SLIDE_DROPOFF) <= 8) && (Math.abs(tilt_arm.getCurrentPosition() - TILT_HIGH) <= 5)) {
                        liftTimer.reset();
                        tilt_claw.setPosition(1);
                        //liftState = LiftState.LIFT_INC;
                        break;
                    }
                }
                break;
            case LIFT_DROPCYCLE:
                tilt_arm.setPower(1);
                tilt_arm.setTargetPosition(TILT_HIGH);
                if (tilt_arm.getCurrentPosition() >= 180) {
                    rotate_arm.setTargetPosition(ROTATE_DROP);
                    if (Math.abs(rotate_arm.getCurrentPosition() - ROTATE_DROP) <= 30) {
                        slide_extension.setTargetPosition(SLIDE_DROPOFF);
                        tilt_claw.setPosition(CLAWTILT_DEPOSIT);
                        if ((Math.abs(slide_extension.getCurrentPosition() - SLIDE_DROPOFF) <= 8) && (Math.abs(tilt_arm.getCurrentPosition() - TILT_HIGH) <= 5)) {
                            claw.setPosition(CLAW_DEPOSIT);
                            liftTimer.reset();
                            liftState = LiftState.LIFT_INC;
                            break;
                        }
                    }
                }
                break;

            case LIFT_GETNEW:
                tilt_claw.setPosition(CLAWTILT_COLLECT);
                rotate_arm.setTargetPosition(ROTATE_COLLECT);
                if (Math.abs(rotate_arm.getCurrentPosition() - ROTATE_COLLECT) <= 100){
                    tilt_arm.setPower(-0.2);
                    tilt_arm.setTargetPosition(TILT_LOW);
                    if (tilt_arm.getCurrentPosition() - TILT_LOW <= 3){
                        slide_extension.setTargetPosition(SLIDE_COLLECT);
                        if (slide_extension.getCurrentPosition() >= (SLIDE_COLLECT-8) && tilt_arm.getCurrentPosition() - TILT_LOW <= 3) {
                            claw.setPosition(CLAW_HOLD);
                            liftTimer.reset();
                            liftState = LiftState.LIFT_HOLD;
                            break;
                        }
                    }
                }
                break;

            case LIFT_HOLD:
                if (liftTimer.seconds() >= 0.4) {
                    liftState = LiftState.LIFT_DROPCYCLE;
                    break;
                }
                break;

            case LIFT_INC:
                if (cones_dropped <= CONES_DESIRED) {
                    if (liftTimer.seconds() >= 0.5) {
                        cones_dropped += 1;
                        TILT_LOW = TILT_LOW-20;
                        liftTimer.reset();
                        liftState = LiftState.LIFT_RETRACTSLIDE;
                        break;
                    }
                }
                else {
                    if (liftTimer.seconds() >= 0.5) {

                        liftTimer.reset();
                        liftState = LiftState.PARKING_STATE;
                        break;
                    }
                }
                break;
            case LIFT_RETRACTSLIDE:
                slide_extension.setTargetPosition(SLIDE_LOW);
                rotate_arm.setTargetPosition(ROTATE_COLLECT);
                if (slide_extension.getCurrentPosition() <= 1100) {
                    liftTimer.reset();
                    //liftState = LiftState.LIFT_GETNEW;
                    liftState = LiftState.LIFT_GETNEW;
                    break;
                }
                break;
            case PARKING_STATE:
                liftTimer.reset();
                liftState = LiftState.FINISH;

                break;
                /*// Use the parkingTag here - it must be at least LEFT if no tag was seen
                if (parkingTag == LEFT){ //&& cones_dropped >= CONES_DESIRED) {

                    drive.followTrajectorySequenceAsync(BlueOnRedGoLeft);
                    liftTimer.reset();
                    telemetry.addData("test", 1);


                } else if (parkingTag == RIGHT){ //&& cones_dropped >= CONES_DESIRED) {

                    drive.followTrajectorySequenceAsync(BlueOnRedGoRight);
                    liftTimer.reset();
                    telemetry.addData("test", 2);



                } else if (parkingTag == MIDDLE){ //&& cones_dropped >= CONES_DESIRED) {

                    drive.followTrajectorySequenceAsync(BlueOnRedGoMiddle);
                    liftTimer.reset();
                    telemetry.addData("test", 3);

                }
                liftState = LiftState.FINISH;
                break;*/
            case FINISH:
                //drive.update();
                slide_extension.setTargetPosition(0);
                tilt_claw.setPosition(CLAWTILT_END);
                if (liftTimer.seconds() >= 0.5) {
                    rotate_arm.setPower(1);
                    rotate_arm.setTargetPosition(0);
                    tilt_arm.setTargetPosition(0);
                }
                break;



        }
    }

    void tagToTelemetry(AprilTagDetection detection)
    {
        telemetry.addLine(String.format("\nDetected tag ID=%d", detection.id));
        telemetry.addLine(String.format("Translation X: %.2f feet", detection.pose.x*FEET_PER_METER));
        telemetry.addLine(String.format("Translation Y: %.2f feet", detection.pose.y*FEET_PER_METER));
        telemetry.addLine(String.format("Translation Z: %.2f feet", detection.pose.z*FEET_PER_METER));
        telemetry.addLine(String.format("Rotation Yaw: %.2f degrees", Math.toDegrees(detection.pose.yaw)));
        telemetry.addLine(String.format("Rotation Pitch: %.2f degrees", Math.toDegrees(detection.pose.pitch)));
        telemetry.addLine(String.format("Rotation Roll: %.2f degrees", Math.toDegrees(detection.pose.roll)));
    }
}