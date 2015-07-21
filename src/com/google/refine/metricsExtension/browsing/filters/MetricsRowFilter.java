/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.metricsExtension.browsing.filters;

import java.util.Collection;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.refine.browsing.RowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Determine if a row should be displayed based on if a metric determined that a value in
 * the row is violating its constraints. Rows should only be displayed if they satisfy the
 * error state of every metric selected or only one, based on the mode property. 
 */
public class MetricsRowFilter<E> implements RowFilter {
    final protected Metric<E>[]        _metrics; // the expression to evaluate
    
    final protected String          _columnName;
    final protected int             _cellIndex; // the expression is based on this column;
                                                // -1 if based on no column in particular,
                                                // for expression such as "row.starred".
    
    final protected Object[]        _matches;
    final protected boolean         _selectBlank;
    final protected boolean         _selectError;
    final protected boolean         _invert;
    
    public MetricsRowFilter(
        Metric<E>[] metrics,
        String columnName,
        int cellIndex, 
        Object[] matches, 
        boolean selectBlank, 
        boolean selectError,
        boolean invert
    ) {
        _metrics = metrics;
        _columnName = columnName;
        _cellIndex = cellIndex;
        _matches = matches;
        _selectBlank = selectBlank;
        _selectError = selectError;
        _invert = invert;
    }

    @Override
    public boolean filterRow(Project project, int rowIndex, Row row) {
        return internalFilterRow(project, rowIndex, row, _invert);
    }
    
    public boolean internalFilterRow(Project project, int rowIndex, Row row, boolean invert) {
        Cell cell = _cellIndex < 0 ? null : row.getCell(_cellIndex);
        
        Properties bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);
        
        Object value = _evaluable.evaluate(bindings);
        if (value != null) {
            if (value.getClass().isArray()) {
                Object[] a = (Object[]) value;
                for (Object v : a) {
                    if (testValue(v)) {
                        return (true & !invert);
                    }
                }
                return (false & !invert);
            } else if (value instanceof Collection<?>) {
                for (Object v : ExpressionUtils.toObjectCollection(value)) {
                    if (testValue(v)) {
                        return (true & !invert);
                    }
                }
                return (false & !invert);
            } else if (value instanceof JSONArray) {
                JSONArray a = (JSONArray) value;
                int l = a.length();
                
                for (int i = 0; i < l; i++) {
                    try {
                        if (testValue(a.get(i))) {
                            return (true & !invert);
                        }
                    } catch (JSONException e) {
                        // ignore
                    }
                }
                return (false & !invert);
            } // else, fall through
        }
        
        return (testValue(value) & !invert);
    }

    protected boolean testValue(Object v) {
        if (ExpressionUtils.isError(v)) {
            return _selectError;
        } else if (ExpressionUtils.isNonBlankData(v)) {
            for (Object match : _matches) {
                if (testValue(v, match)) {
                    return true;
                }
            }
            return false;
        } else {
            return _selectBlank;
        }
    }
    
    protected boolean testValue(Object v, Object match) {
        return (v instanceof Number && match instanceof Number) ?
                ((Number) match).doubleValue() == ((Number) v).doubleValue() :
                match.equals(v);
    }
}
