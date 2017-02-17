package mech.androidthings.samplepwmled;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

/**
 * Created by Mech on 17.02.2017.
 */
public class SampleActivity extends Activity {
    private static final String TAG = SampleActivity.class.getSimpleName();

    private static final String PWM_NAME = "PWM0";
    private static final String BUTTON_GPIO_NAME = "BCM21";

    private Pwm mLedPwm;
    private Gpio mButtonGpio;

    private int mPwmDutyCycle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            Log.i(TAG, "Configuring GPIO pins");

            mLedPwm = pioService.openPwm(PWM_NAME);
            mLedPwm.setPwmFrequencyHz(120);
            mLedPwm.setPwmDutyCycle(mPwmDutyCycle);

            // Enable the PWM signal
            mLedPwm.setEnabled(true);

            Log.i(TAG, "Configuring LED pin success");

            mButtonGpio = pioService.openGpio(BUTTON_GPIO_NAME);

            // Initialize the pin as an input
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            // Low voltage is considered active
            mButtonGpio.setActiveType(Gpio.ACTIVE_LOW);

            // Register for all state changes
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);

            Log.i(TAG, "Configuring button pin success");

        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Begin listening for interrupt events
        try {
            mButtonGpio.registerGpioCallback(mGpioCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Interrupt events no longer necessary
        mButtonGpio.unregisterGpioCallback(mGpioCallback);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (mButtonGpio != null) {
            try {
                mButtonGpio.close();
                mButtonGpio = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close button pin", e);
            }
        }

        if (mLedPwm != null) {
            try {
                mLedPwm.close();
                mLedPwm = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close PWM", e);
            }
        }
    }

    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            mPwmDutyCycle += 25;

            if (mPwmDutyCycle > 100) {
                mPwmDutyCycle = 0;
            }

            try {
                mLedPwm.setPwmDutyCycle(mPwmDutyCycle);
            } catch (IOException e) {
                Log.e(TAG, "Error LED pin change", e);
            }

            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };
}