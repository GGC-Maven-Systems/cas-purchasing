package ph.com.guanzongroup.cas.purchasing.validator;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * In-memory ResultSet for tests: rows of column-name to value maps.
 * Supports next(), getString/getInt/getDouble/getObject by label, wasNull()
 * and close(); anything else throws UnsupportedOperationException.
 */
final class FakeResultSet {

    private FakeResultSet() {
    }

    @SafeVarargs
    static ResultSet of(Map<String, Object>... faRows) {
        final List<Map<String, Object>> laRows = new ArrayList<>(Arrays.asList(faRows));
        final int[] lnRow = {-1};

        return (ResultSet) Proxy.newProxyInstance(FakeResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next":
                            lnRow[0]++;
                            return lnRow[0] < laRows.size();
                        case "close":
                            return null;
                        case "isClosed":
                            return false;
                        case "wasNull":
                            return false;
                        case "getString": {
                            Object loValue = laRows.get(lnRow[0]).get((String) args[0]);
                            return loValue == null ? null : loValue.toString();
                        }
                        case "getInt": {
                            Object loValue = laRows.get(lnRow[0]).get((String) args[0]);
                            return loValue == null ? 0 : ((Number) loValue).intValue();
                        }
                        case "getDouble": {
                            Object loValue = laRows.get(lnRow[0]).get((String) args[0]);
                            return loValue == null ? 0.0 : ((Number) loValue).doubleValue();
                        }
                        case "getObject":
                            return laRows.get(lnRow[0]).get((String) args[0]);
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "FakeResultSet" + laRows;
                        default:
                            throw new UnsupportedOperationException("FakeResultSet." + method.getName());
                    }
                });
    }

    static Map<String, Object> row(Object... faKeyValues) {
        Map<String, Object> loRow = new java.util.HashMap<>();
        for (int lnCtr = 0; lnCtr < faKeyValues.length; lnCtr += 2) {
            loRow.put((String) faKeyValues[lnCtr], faKeyValues[lnCtr + 1]);
        }
        return loRow;
    }
}
