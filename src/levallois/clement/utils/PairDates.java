/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package levallois.clement.utils;

import org.joda.time.LocalDate;

/**
 *
 * @author Clement
 */
public class PairDates<left,right> implements Comparable<PairDates<left,right>> {

    private final LocalDate left;
    private final LocalDate right;

    public PairDates(LocalDate left, LocalDate right) {
        this.left = left;
        this.right = right;
    }

    public LocalDate getLeft() {
        return left;
    }

    public LocalDate getRight() {
        return right;
    }


    // todo move this to a helper class.
    private static int compare(Object o1, Object o2) {
        return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1 : ((Comparable) o1).compareTo(o2);
    }

    @Override
    public int compareTo(PairDates o) {
        int cmp = compare(this.left, o.left);
        return cmp == 0 ? compare(left, o.left) : cmp;
    }
}