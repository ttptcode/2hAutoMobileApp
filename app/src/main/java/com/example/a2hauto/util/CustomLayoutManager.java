package com.example.a2hauto.util;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Custom LayoutManager to disable RecyclerView scrolling
 * and let parent ScrollView handle all scrolling.
 * This allows all items to be displayed without height restrictions.
 */
public class CustomLayoutManager extends LinearLayoutManager {

    public CustomLayoutManager(android.content.Context context) {
        super(context);
    }

    @Override
    public boolean canScrollVertically() {
        // Disable parent RecyclerView scrolling
        // Parent ScrollView will handle all scrolling
        return false;
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                          int widthSpec, int heightSpec) {
        // Measure all items and expand height to fit all of them
        int totalHeight = 0;
        int itemCount = getItemCount();

        for (int i = 0; i < itemCount; i++) {
            try {
                View view = recycler.getViewForPosition(i);
                measureChildWithMargins(view, 0, 0);
                totalHeight += getDecoratedMeasuredHeight(view);
                recycler.recycleView(view);
            } catch (IndexOutOfBoundsException e) {
                // If the adapter size changed during measurement
                break;
            }
        }

        // Set the measured dimensions with total height
        setMeasuredDimension(View.MeasureSpec.getSize(widthSpec), totalHeight);
    }
}
