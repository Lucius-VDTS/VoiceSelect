package ca.vdts.voiceselect.library.interfaces;

/**
 * Interface used to filter entities based on a string criteria.
 */
public interface VDTSFilterableInterface {
    /**
     * concatenate the current filter with another filter. If there is no current filter,
     * then this function acts as if {@link #filter(String)} was called.
     *
     * @param criteria the string to add to the current filter.
     */
    void addFilter(String criteria);

    /**
     * Filter entities in the dataset whose names start with the input criteria. If the criteria is
     * the same as the current filter criteria then this function does nothing.
     *
     * @param criteria the string to filter by.
     */
    void filter(String criteria);


    /**
     * Clears the filter, restoring the unfiltered dataset.
     * If there was no filter this function does nothing.
     */
    void clearFilter();

    String getFilter();
}
