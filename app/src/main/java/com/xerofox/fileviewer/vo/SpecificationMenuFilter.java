package com.xerofox.fileviewer.vo;

public class SpecificationMenuFilter implements MenuFilter {
    public static final int MIN = Integer.MIN_VALUE;
    public static final int MAX = Integer.MAX_VALUE;
    private int min;
    private int max;

    public SpecificationMenuFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getText() {
        if (min == MIN && max == MAX) {
            return CLEAR;
        }
        StringBuilder sb = new StringBuilder();
        if (min != MIN) {
            sb.append(min).append("≤");
        }
        sb.append("肢宽");
        if (max != MAX) {
            sb.append("≤").append(max);
        }
        return sb.toString();
    }

    @Override
    public boolean match(TowerPart part) {
        return (min == MIN && max == MAX) || part.getWide() >= min && part.getWide() <= max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpecificationMenuFilter that = (SpecificationMenuFilter) o;

        if (min != that.min) return false;
        return max == that.max;
    }

    @Override
    public int hashCode() {
        int result = min;
        result = 31 * result + max;
        return result;
    }
}
