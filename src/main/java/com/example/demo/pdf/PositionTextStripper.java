package com.example.demo.pdf;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PositionTextStripper extends PDFTextStripper {

    private final List<TextLocation> locations = new ArrayList<>();
    private int pageIndex = 0;

    public PositionTextStripper() throws IOException {
        setSortByPosition(true);
    }

    @Override
    protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) {
        pageIndex++;
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {

        if (positions.isEmpty()) return;

        StringBuilder word = new StringBuilder();
        float startX = 0, startY = 0, width = 0, height = 0;

        TextPosition prev = null;

        for (TextPosition pos : positions) {

            if (prev == null) {
                startX = pos.getXDirAdj();
                startY = pos.getYDirAdj();
                height = pos.getHeightDir();
                word.append(pos.getUnicode());
                width = pos.getWidthDirAdj();
            } else {
                float gap = pos.getXDirAdj() - (prev.getXDirAdj() + prev.getWidthDirAdj());

                // GAP > threshold = new word
                if (gap > prev.getWidthDirAdj() * 0.5) {
                    locations.add(new TextLocation(
                            pageIndex - 1,
                            startX,
                            startY,
                            width,
                            height,
                            word.toString()
                    ));

                    word.setLength(0);
                    startX = pos.getXDirAdj();
                    startY = pos.getYDirAdj();
                    width = 0;
                }

                word.append(pos.getUnicode());
                width += pos.getWidthDirAdj();
            }

            prev = pos;
        }

        // flush last word
        if (!word.isEmpty()) {
            locations.add(new TextLocation(
                    pageIndex - 1,
                    startX,
                    startY,
                    width,
                    height,
                    word.toString()
            ));
        }
    }

    public List<TextLocation> getLocations() {
        return locations;
    }
}
