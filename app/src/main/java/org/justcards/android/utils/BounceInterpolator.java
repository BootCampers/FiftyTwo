package org.justcards.android.utils;

import android.view.animation.Interpolator;

/**
 * Created by baphna on 4/13/2017.
 * Credit: http://evgenii.com/blog/spring-button-animation-on-android/
 */

public class BounceInterpolator implements Interpolator {

    double mAmplitude = 1;
    double mFrequency = 10;

    BounceInterpolator(double amplitude, double frequency) {
        mAmplitude = amplitude;
        mFrequency = frequency;
    }

    public float getInterpolation(float time) {
        return (float) (-1 * Math.pow(Math.E, -time/ mAmplitude) *
                Math.cos(mFrequency * time) + 1);
    }
}
