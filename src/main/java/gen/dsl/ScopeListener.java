package gen.dsl;

import java.util.Map;

public interface ScopeListener
{
    void addToScope(String scopeName, Map<String, Object> scopedVariables);
}
