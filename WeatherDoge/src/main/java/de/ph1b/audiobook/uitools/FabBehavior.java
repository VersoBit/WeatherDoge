/*
    Copyright (C) 2015 Paul Woitaschek

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    https://github.com/futuresimple/android-floating-action-button/issues/209#issuecomment-124835307
*/

package de.ph1b.audiobook.uitools;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.List;

/**
 * Created by Paul Woitaschek (http://www.paul-woitaschek.de, woitaschek@posteo.de)
 * Defines the behavior for the floating action button. If the dependency is a Snackbar, move the
 * fab up.
 */
@SuppressWarnings("ALL")
public class FabBehavior extends CoordinatorLayout.Behavior<FloatingActionsMenu> {

    private float mTranslationY;

    @SuppressWarnings("unused")
    public FabBehavior() {
        super();
    }

    @SuppressWarnings("unused")
    public FabBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static float getFabTranslationYForSnackbar(CoordinatorLayout parent, FloatingActionsMenu fab) {
        float minOffset = 0.0F;
        List dependencies = parent.getDependencies(fab);
        int i = 0;

        for (int z = dependencies.size(); i < z; ++i) {
            View view = (View) dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float) view.getHeight());
            }
        }

        return minOffset;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionsMenu child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionsMenu child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            this.updateFabTranslationForSnackbar(parent, child, dependency);
        }

        return false;
    }

    private void updateFabTranslationForSnackbar(CoordinatorLayout parent, FloatingActionsMenu fab, View snackbar) {
        float translationY = FabBehavior.getFabTranslationYForSnackbar(parent, fab);
        if (translationY != this.mTranslationY) {
            ViewCompat.animate(fab).cancel();
            if (Math.abs(translationY - this.mTranslationY) == (float) snackbar.getHeight()) {
                ViewCompat.animate(fab).translationY(translationY).setInterpolator(new FastOutSlowInInterpolator()).setListener(null);
            } else {
                ViewCompat.setTranslationY(fab, translationY);
            }

            this.mTranslationY = translationY;
        }

    }
}
