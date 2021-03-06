package com.google.refine.metricsExtension.expr.metrics.spanningColumn;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.CellTuple;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.WrappedCell;

public class DateInterval implements SpanningColumnMetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {"lteq", "0", "seconds"});
	
	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 2) {
			WrappedCell valFrom = (WrappedCell) ((CellTuple) bindings.get("cells")).getField((String) args[0], bindings);
			WrappedCell valTo = (WrappedCell) ((CellTuple) bindings.get("cells")).getField((String) args[1], bindings);
			
			if (valFrom.cell.value.toString().isEmpty() || valFrom.cell.value.toString().isEmpty()) {
				return false;
			}
//			int isAfter;
			if (!(valFrom.cell.value instanceof Date && valTo.cell.value instanceof Date) && !(valFrom.cell.value instanceof Calendar && valTo.cell.value instanceof Calendar)) {
				return false;
			}
			
			long delta = (((Date) valTo.cell.value).getTime() - ((Date) valFrom.cell.value).getTime()) / 1000;
			long threshold = 0;
			String unit = "";
			if (args.length >= 3) {
				String comp = (String) args[2];
				if(!Arrays.asList(new String[]{"gteq", "lteq", "gt", "lt", "eq"}).contains(comp)) {
					return new EvalError("Unknown comparison " + comp);
				}
				if(args.length >= 4) {
					threshold = (long) args[3];
					unit = (String) args[4];
					if ("seconds".equals(unit)) {
                        return validateDiff(comp, delta, threshold);
                    }
                    delta /= 60;
                    if ("minutes".equals(unit)) {
                    	return validateDiff(comp, delta, threshold);
                    }
                    delta /= 60;
                    if ("hours".equals(unit)) {
                    	return validateDiff(comp, delta, threshold);
                    }
                    long days = delta / 24;
                    if ("days".equals(unit)) {
                    	return validateDiff(comp, days, threshold);
                    }
                    if ("weeks".equals(unit)) {
                    	return validateDiff(comp, (days/7), threshold);
                    }
                    if ("months".equals(unit)) {
                    	return validateDiff(comp, (days/30), threshold);
                    }
                    if ("years".equals(unit)) {
                    	return validateDiff(comp, (days/365), threshold);
                    }
                    return new EvalError("Unknown time unit " + unit);
				}
				return validateDiff(comp, delta, threshold);
			} else {
				return delta <= 0;
			}
		}
		return false;
	}
	
	private boolean validateDiff(String comp, long delta, long threshold) {
		if("gteq".equals(comp)) {
			return delta >= threshold;
		} else if ("lteq".equals(comp)) {
			return delta <= threshold;
		} else if ("gt".equals(comp)) {
			return delta > threshold;
		} else if ("lt".equals(comp)) {
			return delta < threshold;
		} else if ("eq".equals(comp)){
			return delta == threshold;
		} else {
			// default state, will not get reached
			return false;
		}
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
        writer.object();
        writer.key("description"); writer.value(getDescription());
        writer.key("params"); writer.value(getParams());
        writer.key("returns"); writer.value("boolean");
        writer.key("defaultParams"); writer.value(defaultParams.toString());
        writer.endObject();
	}

	@Override
	public String getDescription() {
		return "Determine if an interval is negative";
	}

	@Override
	public Evaluable getEvaluable(String[] columns, String[] params) throws ParsingException {
		String eval = "dateInterval(";
		Iterator<String> it = Arrays.asList(columns).iterator();
		while(it.hasNext()) {
			eval += "\"" + it.next() + "\"";
			if(it.hasNext()) {
				eval += ", ";
			}
		}
		if (params != null) {
			it = Arrays.asList(params).iterator();
		} else {
			it = defaultParams.iterator();
		}
		eval += ", ";
		while (it.hasNext()) {
			eval += "\"" + it.next() + "\"";
			if (it.hasNext()) {
				eval += ", ";
			}
		}
		eval += ")";
		return MetaParser.parse(eval);
	}

	@Override
	public String getParams() {
		return "start column, end column, [gteq (greater than or equal) | eq (equal) | lteq (less than or equal) | gt (greater than) | lt (lesser than)] (optional), number value (optional), string timeunit (optional)";
	}
}
