package com.david.reactivecache;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DistinctTest {

    @Test
    void test() {
        assertThat(distinct(List.of(2, 1, 1, 2, 3))).isEqualTo(List.of(2, 1, 3));
    }

    /* I could put this into scope service - that would make testing the Authentication
     * Service easier
     */
    <T> List<T> distinct(List<T> values) {
        //compare by name
        //Comparator<T> comparator = null;

        ///  Comparator<Employee> employeeNameComparator = Comparator.comparing(Employee::getName);

        //var set = new TreeSet<>(comparator);
        var set = new LinkedHashSet<T>();
        set.addAll(values);
        return set.stream().toList();
    }

    @Test
    void testPartition() {
        List<Integer> availablScopes = List.of(1, 2, 3, 4, 5);
        List<String> availableChatbotScopeNames = availablScopes.stream().filter(s -> s instanceof Integer).map(Object::toString).toList();

        List<String> requestedScopes = List.of();
        Map<Boolean, List<String>> requestedScopesPartitioned = requestedScopes.stream().collect(Collectors.partitioningBy(availableChatbotScopeNames::contains));
        List<String> requestedChatbotScopes = requestedScopesPartitioned.get(true);
        List<String> requestedNonChatbotScopes = requestedScopesPartitioned.get(false);

        assertThat(requestedChatbotScopes).isNotNull();
        assertThat(requestedNonChatbotScopes).isNotNull();


    }

}