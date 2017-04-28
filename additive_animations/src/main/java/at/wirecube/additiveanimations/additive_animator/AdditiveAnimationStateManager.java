package at.wirecube.additiveanimations.additive_animator;

import android.util.Property;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A class that manages internal values about the state of all running additive animations for a single view.
 */
class AdditiveAnimationStateManager {

    private static final Map<View, AdditiveAnimationStateManager> sStateManagers = new HashMap<>();

    static final AdditiveAnimationStateManager from(View targetView) {
        if(targetView == null) {
            return null;
        }
        AdditiveAnimationStateManager animator = sStateManagers.get(targetView);
        if (animator == null) {
            animator = new AdditiveAnimationStateManager(targetView);
            sStateManagers.put(targetView, animator);
        }
        return animator;
    }

    static final AnimationAccumulator getAccumulatedProperties(View v) {
        return from(v).mAccumulator;
    }

    private final AnimationAccumulator mAccumulator = new AnimationAccumulator();

    private final View mAnimationTargetView;
    // sorted: last = newest
    private final List<AdditiveAnimationApplier> mAdditiveAnimationAppliers = new ArrayList<>();
    private final Map<String, Float> mLastTargetValues = new HashMap<>();

    private AdditiveAnimationStateManager(View animationTarget) {
        mAnimationTargetView = animationTarget;
    }

    Float getLastTargetValue(String propertyName) {
        return mLastTargetValues.get(propertyName);
    }

    Float getActualPropertyValue(Property<View, Float> property) {
        Float lastTarget = getLastTargetValue(property.getName());
        if(lastTarget == null) {
            lastTarget = property.get(mAnimationTargetView);
        }
        return lastTarget;
    }

    void addAnimation(AdditiveAnimationApplier animationApplier, AdditiveAnimation animation) {
        if(getLastTargetValue(animation.getTag()) == null) {
            mAccumulator.getAccumulatedProperties().put(animation, animation.getStartValue());
        } else {
            animation.setStartValue(getLastTargetValue(animation.getTag()));
        }
        mLastTargetValues.put(animation.getTag(), animation.getTargetValue());
        // immediately add to our list of pending animators
        mAdditiveAnimationAppliers.add(animationApplier);
        animationApplier.addAnimation(animation);
    }

    void onAnimationApplierEnd(AdditiveAnimationApplier applier) {
        mAdditiveAnimationAppliers.remove(applier);
        if (mAdditiveAnimationAppliers.isEmpty()) {
            sStateManagers.remove(mAnimationTargetView);
        }
        mAccumulator.totalNumAnimationUpdaters--;
    }

    void onAnimationApplierStart(AdditiveAnimationApplier applier) {
        // only now are we expecting updates from this applier
        mAccumulator.totalNumAnimationUpdaters++;
    }

    void cancelAllAnimations() {
        for(AdditiveAnimationApplier additiveAnimationApplier : mAdditiveAnimationAppliers) {
            additiveAnimationApplier.cancel(mAnimationTargetView);
        }
        mAdditiveAnimationAppliers.clear();
        mLastTargetValues.clear();
        sStateManagers.remove(mAnimationTargetView);
    }

    void cancelAnimation(String propertyName) {
        List<AdditiveAnimationApplier> cancelledAppliers = new ArrayList<>();
        for(AdditiveAnimationApplier applier : mAdditiveAnimationAppliers) {
            if(applier.removeAnimation(propertyName, mAnimationTargetView)) {
                cancelledAppliers.add(applier);
            }
        }
        mLastTargetValues.remove(propertyName);
        mAdditiveAnimationAppliers.removeAll(cancelledAppliers);
    }
}