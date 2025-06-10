package group.rohlik.grocerymanager.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the BatchUtil class.
 *
 * @author Tomas Kramec
 */
class BatchUtilTest {

    @Test
    void splitIntoBatches_emptyList_returnsEmptyList() {
        List<Integer> input = Collections.emptyList();
        List<List<Integer>> result = BatchUtil.splitIntoBatches(input, 3);
        assertThat(result).isEmpty();
    }

    @Test
    void splitIntoBatches_batchSizeGreaterThanList_returnsOneBatch() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<List<Integer>> result = BatchUtil.splitIntoBatches(input, 10);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly(1, 2, 3);
    }

    @Test
    void splitIntoBatches_exactBatches() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4);
        List<List<Integer>> result = BatchUtil.splitIntoBatches(input, 2);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(1, 2);
        assertThat(result.get(1)).containsExactly(3, 4);
    }

    @Test
    void splitIntoBatches_lastBatchSmaller() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> result = BatchUtil.splitIntoBatches(input, 2);
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly(1, 2);
        assertThat(result.get(1)).containsExactly(3, 4);
        assertThat(result.get(2)).containsExactly(5);
    }

    @Test
    void splitIntoBatches_batchSizeOne() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<List<Integer>> result = BatchUtil.splitIntoBatches(input, 1);
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly(1);
        assertThat(result.get(1)).containsExactly(2);
        assertThat(result.get(2)).containsExactly(3);
    }
}
