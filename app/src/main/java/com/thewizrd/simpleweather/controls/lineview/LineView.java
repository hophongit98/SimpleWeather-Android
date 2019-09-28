package com.thewizrd.simpleweather.controls.lineview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineView extends HorizontalScrollView {

    private HorizontalScrollView mScrollViewer;
    private RectF visibleRect;
    private LineViewGraph graph;
    private OnClickListener onClickListener;

    public LineView(Context context) {
        super(context);
        initialize(context);
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private void initialize(Context context) {
        graph = new LineViewGraph(context);
        graph.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null)
                    onClickListener.onClick(v);
            }
        });

        this.setFillViewport(true);
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);

        this.removeAllViews();
        this.addView(graph, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mScrollViewer = this;
    }

    public void setDrawGridLines(boolean drawGridLines) {
        this.graph.drawGridLines = drawGridLines;
    }

    public void setDrawDotLine(boolean drawDotLine) {
        this.graph.drawDotLine = drawDotLine;
    }

    public void setDrawDotPoints(boolean drawDotPoints) {
        this.graph.drawDotPoints = drawDotPoints;
    }

    public void setDrawGraphBackground(boolean drawGraphBackground) {
        this.graph.drawGraphBackground = drawGraphBackground;
    }

    public void setDrawIconLabels(boolean drawIconsLabels) {
        this.graph.drawIconsLabels = drawIconsLabels;
    }

    public void setDrawDataLabels(boolean drawDataLabels) {
        this.graph.drawDataLabels = drawDataLabels;
    }

    public void setDrawSeriesLabels(boolean drawSeriesLabels) {
        this.graph.drawSeriesLabels = drawSeriesLabels;
    }

    public void setData(List<XLabelData> dataLabels, List<LineDataSeries> dataLists) {
        this.graph.setDataLabels(dataLabels);
        this.graph.setDataList(dataLists);
    }

    public void setBackgroundLineColor(@ColorInt int color) {
        this.graph.BACKGROUND_LINE_COLOR = color;
    }

    public void setBottomTextColor(@ColorInt int color) {
        this.graph.BOTTOM_TEXT_COLOR = color;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
            this.graph.iconPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
        }
    }

    public void setLineColor(@ColorInt int color) {
        this.graph.LINE_COLOR = color;
    }

    @Override
    protected void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);
        if (visibleRect == null) visibleRect = new RectF();
        visibleRect.set(scrollX, scrollY, scrollX + this.getWidth(), scrollY + this.getHeight());
        this.graph.invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.graph.invalidate();
        visibleRect = null;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Invalidate the visible rect
        visibleRect = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.graph.setOnTouchListener(l);
    }

    public final int getItemPositionFromPoint(float xCoordinate) {
        return this.graph.getItemPositionFromPoint(xCoordinate);
    }

    /*
     *  Multi-series line graph
     *  Based on LineView from http://www.androidtrainee.com/draw-android-line-chart-with-animation/
     *  Graph background (under line) based on - https://github.com/jjoe64/GraphView (LineGraphSeries)
     */
    private class LineViewGraph extends View {
        private int mViewHeight;
        private int mViewWidth;
        // Containers to check if we're drawing w/in bounds
        private RectF drawingRect;
        private float drwTextWidth;
        //drawBackground
        private int dataOfAGird = 10;
        private float bottomTextHeight = 0;
        private List<XLabelData> dataLabels; // X
        private List<LineDataSeries> dataLists; // Y data

        private List<Float> xCoordinateList;
        private List<Float> yCoordinateList;

        private List<List<Dot>> drawDotLists;

        private Paint bottomTextPaint;
        private int bottomTextDescent;

        private TextPaint iconPaint;

        private final float iconBottomMargin = ActivityUtils.dpToPx(getContext(), 2);
        private final float bottomTextTopMargin = ActivityUtils.dpToPx(getContext(), 6);
        private final float bottomLineLength = ActivityUtils.dpToPx(getContext(), 22);
        private final float DOT_INNER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 2);
        private final float DOT_OUTER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 5);
        private final float MIN_TOP_LINE_LENGTH = ActivityUtils.dpToPx(getContext(), 12);
        private final float LEGEND_MARGIN_HEIGHT = ActivityUtils.dpToPx(getContext(), 20);
        private int BACKGROUND_LINE_COLOR = Colors.WHITESMOKE;
        private int BOTTOM_TEXT_COLOR = Colors.WHITE;
        private int LINE_COLOR = Colors.BLACK;

        private float topLineLength = MIN_TOP_LINE_LENGTH;
        private float sideLineLength = ActivityUtils.dpToPx(getContext(), 45) / 3 * 2;
        private float backgroundGridWidth = ActivityUtils.dpToPx(getContext(), 45);
        private float longestTextWidth;

        private int[] colorArray = {Colors.SIMPLEBLUE, Colors.LIGHTSEAGREEN, Colors.YELLOWGREEN};

        private boolean drawGridLines = false;
        private boolean drawDotLine = false;
        private boolean drawDotPoints = false;
        private boolean drawGraphBackground = false;
        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;
        private boolean drawSeriesLabels = false;

        LineViewGraph(Context context) {
            this(context, null);
        }

        LineViewGraph(Context context, AttributeSet attrs) {
            super(context, attrs);
            bottomTextPaint = new Paint();
            dataLabels = new ArrayList<>();
            dataLists = new ArrayList<>();
            xCoordinateList = new ArrayList<>();
            yCoordinateList = new ArrayList<>();
            drawDotLists = new ArrayList<>();

            bottomTextPaint.setAntiAlias(true);
            bottomTextPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 12));
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);
            bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
            bottomTextPaint.setShadowLayer(1, 1, 1, ColorsUtils.isSuperLight(BOTTOM_TEXT_COLOR) ? Colors.BLACK : Colors.GRAY);

            iconPaint = new TextPaint();
            iconPaint.setAntiAlias(true);
            iconPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 24));
            iconPaint.setTextAlign(Paint.Align.CENTER);
            iconPaint.setStyle(Paint.Style.FILL);
            iconPaint.setColor(BOTTOM_TEXT_COLOR);
            Typeface weathericons = ResourcesCompat.getFont(getContext(), R.font.weathericons);
            iconPaint.setSubpixelText(true);
            iconPaint.setTypeface(weathericons);
            iconPaint.setShadowLayer(1, 1, 1, ColorsUtils.isSuperLight(BOTTOM_TEXT_COLOR) ? Colors.BLACK : Colors.GRAY);
        }

        private int getAdj() {
            int adj = 1;
            if (drawIconsLabels) adj = 2; // Make space for icon labels

            return adj;
        }

        private float getGraphHeight() {
            float graphHeight = mViewHeight - bottomTextTopMargin * getAdj() - bottomTextHeight * getAdj() - bottomLineLength - bottomTextDescent - topLineLength;
            if (drawIconsLabels) graphHeight -= iconBottomMargin;

            return graphHeight;
        }

        private void setDataLabels(List<XLabelData> dataLabels) {
            this.dataLabels.clear();

            if (dataLabels != null) {
                this.dataLabels.addAll(dataLabels);
            }

            if (!drawIconsLabels && dataLabels != null && dataLabels.size() > 0 && !TextUtils.isEmpty(dataLabels.get(0).getIcon()))
                drawIconsLabels = true;
            else if (drawIconsLabels && (dataLabels == null || dataLabels.size() <= 0))
                drawIconsLabels = false;

            Rect r = new Rect();
            int longestWidth = 0;
            String longestStr = "";
            bottomTextDescent = 0;
            longestTextWidth = 0;
            for (XLabelData labelData : this.dataLabels) {
                String s = labelData.getLabel().toString();
                bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                if (bottomTextHeight < r.height()) {
                    bottomTextHeight = r.height();
                }
                if (longestWidth < r.width()) {
                    longestWidth = r.width();
                    longestStr = s;
                }
                if (bottomTextDescent < (Math.abs(r.bottom))) {
                    bottomTextDescent = Math.abs(r.bottom);
                }
            }

            if (longestTextWidth < longestWidth) {
                longestTextWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
            }
            if (sideLineLength < longestWidth / 2f) {
                sideLineLength = longestWidth / 2f;
            }

            backgroundGridWidth = longestTextWidth;

            refreshXCoordinateList(getHorizontalGridNum());
        }

        private void setDataList(List<LineDataSeries> dataLists) {
            this.dataLists.clear();

            if (dataLists != null) {
                this.dataLists.addAll(dataLists);
            }

            for (LineDataSeries series : this.dataLists) {
                if (series.getSeriesData().size() > dataLabels.size()) {
                    throw new RuntimeException("LineView error:" +
                            " seriesData.size() > dataLabels.size() !!!");
                }
            }
            float biggestData = 0;
            for (LineDataSeries series : this.dataLists) {
                for (YEntryData i : series.getSeriesData()) {
                    if (biggestData < i.getY()) {
                        biggestData = i.getY();
                    }
                }
                dataOfAGird = 1;
                while (biggestData / 10 > dataOfAGird) {
                    dataOfAGird *= 10;
                }
            }

            refreshAfterDataChanged();
            setMinimumWidth(0); // It can help the LineView reset the Width,
            // I don't know the better way..
            postInvalidate();
        }

        private void refreshGridWidth() {
            // Reset the grid width
            backgroundGridWidth = longestTextWidth;

            if (getPreferredWidth() < mScrollViewer.getMeasuredWidth()) {
                int freeSpace = mScrollViewer.getMeasuredWidth() - getPreferredWidth();
                int additionalSpace = freeSpace / getHorizontalGridNum();
                backgroundGridWidth += additionalSpace;
                refreshXCoordinateList(getHorizontalGridNum());
            }
        }

        private void refreshAfterDataChanged() {
            float verticalGridNum = getVerticalGridNum();
            refreshTopLineLength(verticalGridNum);
            refreshYCoordinateList(verticalGridNum);
            refreshDrawDotList();
        }

        private float getVerticalGridNum() {
            float verticalGridNum = 4; // int MIN_VERTICAL_GRID_NUM = 4;
            if (dataLists != null && !dataLists.isEmpty()) {
                for (LineDataSeries series : dataLists) {
                    for (YEntryData entry : series.getSeriesData()) {
                        if (verticalGridNum < (entry.getY() + 1)) {
                            verticalGridNum = entry.getY() + 1;
                        }
                    }
                }
            }
            return verticalGridNum;
        }

        private int getHorizontalGridNum() {
            final int MIN_HORIZONTAL_GRID_NUM = 1;
            int horizontalGridNum = dataLabels.size() - 1;
            if (horizontalGridNum < MIN_HORIZONTAL_GRID_NUM) {
                horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            }
            return horizontalGridNum;
        }

        private void refreshXCoordinateList(float horizontalGridNum) {
            xCoordinateList.clear();
            for (int i = 0; i < (horizontalGridNum + 1); i++) {
                xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
            }
        }

        private void refreshYCoordinateList(float verticalGridNum) {
            yCoordinateList.clear();
            for (int i = 0; i < (verticalGridNum + 1); i++) {
                yCoordinateList.add(topLineLength + ((getGraphHeight()) * i / (verticalGridNum)));
            }
        }

        private void refreshDrawDotList() {
            if (dataLists != null && !dataLists.isEmpty()) {
                if (drawDotLists.size() == 0 || drawDotLists.size() != dataLists.size()) {
                    drawDotLists.clear();
                    for (int k = 0; k < dataLists.size(); k++) {
                        drawDotLists.add(new ArrayList<LineViewGraph.Dot>());
                    }
                }
                float maxValue = 0;
                float minValue = 0;
                for (int k = 0; k < dataLists.size(); k++) {
                    float kMax = Collections.max(dataLists.get(k).getSeriesData()).getY();
                    float kMin = Collections.min(dataLists.get(k).getSeriesData()).getY();

                    if (maxValue < kMax)
                        maxValue = kMax;
                    if (minValue > kMin)
                        minValue = kMin;
                }
                for (int k = 0; k < dataLists.size(); k++) {
                    int drawDotSize = drawDotLists.get(k).isEmpty() ? 0 : drawDotLists.get(k).size();

                    for (int i = 0; i < dataLists.get(k).getSeriesData().size(); i++) {
                        float x = xCoordinateList.get(i);
                        // Make space for y data labels
                        float y;
                        if (maxValue == minValue) {
                            y = topLineLength + (getGraphHeight()) / 2f;
                        } else {
                            y = topLineLength + (getGraphHeight()) * (maxValue - dataLists.get(k).getSeriesData().get(i).getY()) / (maxValue - minValue);
                        }

                        // Make space for each series if necessary
                        y += (topLineLength * k * 1.25);
                        if (y >= getGraphHeight()) {
                            y = getGraphHeight();
                        }

                        if (drawSeriesLabels) y += LEGEND_MARGIN_HEIGHT;

                        if (i > drawDotSize - 1) {
                            drawDotLists.get(k).add(new Dot(x, y));
                        } else {
                            drawDotLists.get(k).set(i, new Dot(x, y));
                        }
                    }

                    int temp = drawDotLists.get(k).size() - dataLists.get(k).getSeriesData().size();
                    for (int i = 0; i < temp; i++) {
                        drawDotLists.get(k).remove(drawDotLists.get(k).size() - 1);
                    }
                }
            }
        }

        private void refreshTopLineLength(float verticalGridNum) {
            float labelsize = bottomTextHeight * 2 + bottomTextTopMargin;

            if (drawDataLabels && (getGraphHeight()) /
                    (verticalGridNum + 2) < labelsize) {
                topLineLength = labelsize + 2;
            } else {
                topLineLength = MIN_TOP_LINE_LENGTH;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            visibleRect = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (drawingRect == null) drawingRect = new RectF();

            if (visibleRect == null) {
                visibleRect = new RectF(mScrollViewer.getScrollX(),
                        mScrollViewer.getScrollY(),
                        mScrollViewer.getScrollX() + mScrollViewer.getWidth(),
                        mScrollViewer.getScrollY() + mScrollViewer.getHeight());
            }

            drawBackgroundLines(canvas);
            drawLines(canvas);
            drawDots(canvas);
            drawSeriesLegend(canvas);
        }

        private void drawDots(Canvas canvas) {
            if (drawDotPoints) {
                Paint bigCirPaint = new Paint();
                bigCirPaint.setAntiAlias(true);
                Paint smallCirPaint = new Paint(bigCirPaint);
                smallCirPaint.setColor(LINE_COLOR);
                if (drawDotLists != null && !drawDotLists.isEmpty()) {
                    for (int k = 0; k < drawDotLists.size(); k++) {
                        bigCirPaint.setColor(colorArray[k % 3]);
                        for (Dot dot : drawDotLists.get(k)) {
                            if (visibleRect.contains(dot.x, dot.y)) {
                                canvas.drawCircle(dot.x, dot.y, DOT_OUTER_CIR_RADIUS, bigCirPaint);
                                canvas.drawCircle(dot.x, dot.y, DOT_INNER_CIR_RADIUS, smallCirPaint);
                            }
                        }
                    }
                }
            }
        }

        private void drawLines(Canvas canvas) {
            Paint linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 2));

            float graphHeight = getGraphHeight();
            float graphTop = this.getTop() + topLineLength;

            for (int k = 0; k < drawDotLists.size(); k++) {
                float firstX = -1;
                float firstY = -1;
                // needed to end the path for background
                float lastUsedEndY = 0;

                linePaint.setColor(LINE_COLOR);
                Path mPathBackground = new Path();
                Paint mPaintBackground = new Paint();
                mPaintBackground.setColor(ColorUtils.setAlphaComponent(colorArray[k % 3], 0x50));
                for (int i = 0; i < drawDotLists.get(k).size() - 1; i++) {
                    Dot dot = drawDotLists.get(k).get(i);
                    Dot nextDot = drawDotLists.get(k).get(i + 1);
                    YEntryData entry = dataLists.get(k).getSeriesData().get(i);
                    YEntryData nextEntry = dataLists.get(k).getSeriesData().get(i + 1);

                    float startX = dot.x;
                    float startY = dot.y;
                    float endX = nextDot.x;
                    float endY = nextDot.y;

                    drawingRect.set(0, dot.y, dot.x, dot.y);
                    if (firstX == -1 && RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(0, dot.y, dot.x, dot.y, linePaint);
                    }

                    drawingRect.set(dot.x, dot.y, nextDot.x, nextDot.y);
                    if (RectF.intersects(drawingRect, visibleRect))
                        canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, linePaint);

                    // Draw top label
                    drwTextWidth = bottomTextPaint.measureText(entry.getLabel().toString());
                    drawingRect.set(sideLineLength + backgroundGridWidth * i,
                            dot.y,
                            sideLineLength + backgroundGridWidth * i + drwTextWidth,
                            dot.y + bottomTextHeight);
                    if (drawDataLabels && RectF.intersects(drawingRect, visibleRect))
                        canvas.drawText(entry.getLabel().toString(), sideLineLength + backgroundGridWidth * i, dot.y - bottomTextHeight, bottomTextPaint);

                    if (firstX == -1) {
                        firstX = visibleRect.left;
                        firstY = startY;
                        if (drawGraphBackground)
                            mPathBackground.moveTo(firstX, startY);
                    }

                    drawingRect.set(startX, startY, endX, endY);
                    if (drawGraphBackground && RectF.intersects(drawingRect, visibleRect)) {
                        mPathBackground.lineTo(startX, startY);
                        mPathBackground.lineTo(endX, endY);
                    }

                    // Draw last items
                    if (i + 1 == drawDotLists.get(k).size() - 1) {
                        // Draw top label
                        drwTextWidth = bottomTextPaint.measureText(nextEntry.getLabel().toString());
                        drawingRect.set(sideLineLength + backgroundGridWidth * (i + 1),
                                nextDot.y,
                                sideLineLength + backgroundGridWidth * (i + 1) + drwTextWidth,
                                nextDot.y + bottomTextHeight);

                        if (drawDataLabels && RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(nextEntry.getLabel().toString(), sideLineLength + backgroundGridWidth * (i + 1), nextDot.y - bottomTextHeight, bottomTextPaint);

                        drawingRect.set(nextDot.x, nextDot.y, visibleRect.right, nextDot.y);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawLine(nextDot.x, nextDot.y, visibleRect.right, nextDot.y, linePaint);

                        drawingRect.set(endX, endY, visibleRect.right, endY);
                        if (drawGraphBackground && RectF.intersects(drawingRect, visibleRect)) {
                            mPathBackground.lineTo(endX, endY);
                            mPathBackground.lineTo(visibleRect.right, endY);
                        }
                    }

                    lastUsedEndY = endY;
                }

                if (drawGraphBackground && firstX != -1) {
                    // end / close path
                    if (lastUsedEndY != graphHeight + graphTop) {
                        // dont draw line to same point, otherwise the path is completely broken
                        mPathBackground.lineTo(visibleRect.right, graphHeight + graphTop);
                    }
                    mPathBackground.lineTo(firstX, graphHeight + graphTop);
                    if (firstY != graphHeight + graphTop) {
                        // dont draw line to same point, otherwise the path is completely broken
                        mPathBackground.lineTo(firstX, firstY);
                    }

                    canvas.drawPath(mPathBackground, mPaintBackground);
                }
            }
        }

        private void drawBackgroundLines(Canvas canvas) {
            if (drawGridLines) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 1f));
                paint.setColor(BACKGROUND_LINE_COLOR);
                PathEffect effects = new DashPathEffect(
                        new float[]{10, 5, 10, 5}, 1);

                // draw vertical lines
                for (int i = 0; i < xCoordinateList.size(); i++) {
                    drawingRect.set(xCoordinateList.get(i), 0, xCoordinateList.get(i), getGraphHeight() + topLineLength);

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(xCoordinateList.get(i),
                                0,
                                xCoordinateList.get(i),
                                getGraphHeight() + topLineLength,
                                paint);
                    }
                }

                if (drawDotLine) {
                    // draw dotted lines
                    paint.setPathEffect(effects);
                }

                // draw solid lines
                for (int i = 0; i < yCoordinateList.size(); i++) {
                    drawingRect.set(0, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i));

                    if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0 && RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(0, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i), paint);
                    }
                }
            }

            //draw bottom text
            if (dataLabels != null) {
                for (int i = 0; i < dataLabels.size(); i++) {
                    float x = sideLineLength + backgroundGridWidth * i;
                    float y = mViewHeight - bottomTextDescent;
                    XLabelData xData = dataLabels.get(i);

                    if (!TextUtils.isEmpty(xData.getLabel())) {
                        drwTextWidth = bottomTextPaint.measureText(xData.getLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(xData.getLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (!TextUtils.isEmpty(xData.getIcon())) {
                        int rotation = xData.getIconRotation();
                        String icon = xData.getIcon().toString();
                        Rect r = new Rect();
                        iconPaint.getTextBounds(icon, 0, icon.length(), r);

                        drawingRect.set(x, y, x + r.width(), y + r.height());

                        if (RectF.intersects(drawingRect, visibleRect)) {
                            StaticLayout mTextLayout = new StaticLayout(
                                    icon, iconPaint, r.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                            canvas.save();
                            canvas.translate(x, y - mTextLayout.getHeight() - bottomTextHeight - iconBottomMargin * 2f);
                            canvas.rotate(rotation, 0, mTextLayout.getHeight() / 2f);
                            mTextLayout.draw(canvas);
                            canvas.restore();
                        }
                    }
                }
            }
        }

        private void drawSeriesLegend(Canvas canvas) {
            if (drawSeriesLabels && dataLists.size() > 0) {
                int seriesSize = dataLists.size();

                Rect r = new Rect();
                int longestWidth = 0;
                String longestStr = "";
                float textWidth = 0;
                float paddingLength = 0;
                float textHeight = 0;
                float textDescent = 0;
                for (int i = 0; i < seriesSize; i++) {
                    String title = dataLists.get(i).getSeriesLabel();
                    if (StringUtils.isNullOrWhitespace(title)) {
                        title = "Series " + i;
                    }

                    bottomTextPaint.getTextBounds(title, 0, title.length(), r);
                    if (textHeight < r.height()) {
                        textHeight = r.height();
                    }
                    if (longestWidth < r.width()) {
                        longestWidth = r.width();
                        longestStr = title;
                    }
                    if (textDescent < (Math.abs(r.bottom))) {
                        textDescent = Math.abs(r.bottom);
                    }
                }

                if (textWidth < longestWidth) {
                    textWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
                }
                if (paddingLength < longestWidth / 2f) {
                    paddingLength = longestWidth / 2f;
                }
                textWidth += ActivityUtils.dpToPx(getContext(), 4);

                float rectSize = textHeight;
                float rect2textPadding = ActivityUtils.dpToPx(getContext(), 8);

                for (int i = 0; i < seriesSize; i++) {
                    int seriesColor = colorArray[i % 3];
                    String title = dataLists.get(i).getSeriesLabel();
                    if (StringUtils.isNullOrWhitespace(title)) {
                        title = "Series " + i;
                    }

                    Rect textBounds = new Rect();
                    bottomTextPaint.getTextBounds(title, 0, title.length(), textBounds);

                    float xRectStart = paddingLength + textWidth * i + ((rectSize + rect2textPadding) * i);
                    float xTextStart = paddingLength + textWidth * i + rectSize + ((rectSize + rect2textPadding) * i);

                    RectF rectF = new RectF(xRectStart, bottomTextTopMargin + textDescent, xRectStart + rectSize, rectSize + bottomTextTopMargin + textDescent);
                    Paint rectPaint = new Paint();
                    rectPaint.setAntiAlias(true);
                    rectPaint.setColor(seriesColor);
                    rectPaint.setStyle(Paint.Style.FILL);

                    canvas.drawRect(rectF, rectPaint);
                    canvas.drawText(title, xTextStart + textWidth / 2f, textHeight + bottomTextTopMargin + textDescent, bottomTextPaint);
                }
            }
        }

        private int getItemPositionFromPoint(float xCoordinate) {
            if (getHorizontalGridNum() <= 1) {
                return 0;
            }

            return binarySearchPointIndex(xCoordinate);
        }

        private int binarySearchPointIndex(float targetXPoint) {
            int l = 0;
            int r = xCoordinateList.size() - 1;
            while (l <= r) {
                int midPt = (int) Math.floor((l + r) / 2f);
                if (targetXPoint > (xCoordinateList.get(midPt) - backgroundGridWidth / 2f) && targetXPoint < (xCoordinateList.get(midPt) + backgroundGridWidth / 2f)) {
                    return midPt;
                } else if (targetXPoint <= xCoordinateList.get(midPt) - backgroundGridWidth / 2f) {
                    r = midPt - 1;
                } else if (targetXPoint >= xCoordinateList.get(midPt) + backgroundGridWidth / 2f) {
                    l = midPt + 1;
                }
            }

            return 0;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mViewWidth = measureWidth(widthMeasureSpec);
            mViewHeight = measureHeight(heightMeasureSpec);
            refreshGridWidth();
            refreshAfterDataChanged();
            setMeasuredDimension(mViewWidth, mViewHeight);
        }

        private int getPreferredWidth() {
            int horizontalGridNum = getHorizontalGridNum();
            return (int) (backgroundGridWidth * horizontalGridNum + sideLineLength * 2);
        }

        private int measureWidth(int measureSpec) {
            return getMeasurement(measureSpec, getPreferredWidth());
        }

        private int measureHeight(int measureSpec) {
            int preferred = 0;
            return getMeasurement(measureSpec, preferred);
        }

        private int getMeasurement(int measureSpec, int preferred) {
            int specSize = MeasureSpec.getSize(measureSpec);
            int measurement;
            switch (MeasureSpec.getMode(measureSpec)) {
                case MeasureSpec.EXACTLY:
                    measurement = specSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measurement = Math.min(preferred, specSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measurement = preferred;
                    break;
            }
            return measurement;
        }

        private class Dot {
            float x;
            float y;

            Dot(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}