package org.collectionspace.services.structureddate.antlr;

import java.util.Stack;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.collectionspace.services.structureddate.Date;
import org.collectionspace.services.structureddate.DateUtils;
import org.collectionspace.services.structureddate.Era;
import org.collectionspace.services.structureddate.QualifierType;
import org.collectionspace.services.structureddate.QualifierUnit;
import org.collectionspace.services.structureddate.StructuredDate;
import org.collectionspace.services.structureddate.StructuredDateEvaluator;
import org.collectionspace.services.structureddate.StructuredDateFormatException;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.CircaYearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.CircaYearRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.DateRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.EraContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.MonthRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.NumDayOfMonthContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.NumYearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseDateContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseDateRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseMonthContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseMonthRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseYearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.PreciseYearRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.SmallDateRangeOnlyContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.InvMonthYearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.InvStrDateContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.MonthInYearRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.MonthYearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.NumDateContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.NumDayInMonthRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.NumMonthContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.StrDateContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.StrDayInMonthRangeContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.StrMonthContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.ToDoContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.YearContext;
import org.collectionspace.services.structureddate.antlr.StructuredDateParser.YearRangeContext;

/**
 * A StructuredDateEvaluator that uses an ANTLR parser to parse the display date,
 * and an ANTLR listener to generate a structured date from the resulting parse
 * tree.
 */
public class ANTLRStructuredDateEvaluator extends StructuredDateBaseListener implements StructuredDateEvaluator {
	public static final int FIRST_MONTH = 1;
	public static final int FIRST_DAY_OF_FIRST_MONTH = 1;
	public static final int LAST_MONTH = 12;
	public static final int LAST_DAY_OF_LAST_MONTH = 31;
	
	protected StructuredDate result;
	protected Stack<Object> stack;
	
	public ANTLRStructuredDateEvaluator() {

	}

	@Override
	public StructuredDate evaluate(String displayDate) throws StructuredDateFormatException {
		stack = new Stack<Object>();

		result = new StructuredDate();
		result.setDisplayDate(displayDate);

		ANTLRInputStream inputStream = new ANTLRInputStream(displayDate.toLowerCase());
		StructuredDateLexer lexer = new StructuredDateLexer(inputStream);
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);

		StructuredDateParser parser = new StructuredDateParser(tokenStream);
		parser.setErrorHandler(new BailErrorStrategy());
		parser.removeErrorListeners();
		parser.addParseListener(this);

		try {
			parser.oneDisplayDate();
		}
		catch(ParseCancellationException e) {			
			RecognitionException re = (RecognitionException) e.getCause();
			
			throw new StructuredDateFormatException(getErrorMessage(re), re);
		}
		
		// TODO: Move this somewhere else
		
		if (result.getEarliestSingleDate() != null && result.getEarliestSingleDate().getEra() == null) {
			result.getEarliestSingleDate().setEra(Date.DEFAULT_ERA);
		}
		
		if (result.getLatestDate() != null && result.getLatestDate().getEra() == null) {
			result.getLatestDate().setEra(Date.DEFAULT_ERA);
		}
		
		return result;
	}	

	@Override
	public void exitToDo(ToDoContext ctx) {
		if (ctx.exception != null) return;

		result.setNote("Not yet implemented");
	}

	@Override
	public void exitCircaYear(CircaYearContext ctx) {
		if (ctx.exception != null) return;

		Date yearEndDate = (Date) stack.pop();
		Date yearStartDate = (Date) stack.pop();
		
		// Since this is a single year, the year and era are the same for the start and end date.
		// It doesn't matter which we use to compute the circa interval.
		int interval = DateUtils.getCircaIntervalYears(yearStartDate.getYear(), yearStartDate.getEra());
		
		result.setEarliestSingleDate(yearStartDate.withQualifier(QualifierType.MINUS, interval, QualifierUnit.YEARS));
		result.setLatestDate(yearEndDate.withQualifier(QualifierType.PLUS, interval, QualifierUnit.YEARS));
	}

	@Override
	public void exitPreciseYear(PreciseYearContext ctx) {
		if (ctx.exception != null) return;

		Date yearEndDate = (Date) stack.pop();
		Date yearStartDate = (Date) stack.pop();

		result.setEarliestSingleDate(yearStartDate);		
		result.setLatestDate(yearEndDate);
	}

	@Override
	public void exitCircaYearRange(CircaYearRangeContext ctx) {
		if (ctx.exception != null) return;

		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();

		int earliestInterval = DateUtils.getCircaIntervalYears(earliestDate.getYear(), earliestDate.getEra());
		int latestInterval = DateUtils.getCircaIntervalYears(latestDate.getYear(), latestDate.getEra());
		
		result.setEarliestSingleDate(earliestDate.withQualifier(QualifierType.MINUS, earliestInterval, QualifierUnit.YEARS));
		result.setLatestDate(latestDate.withQualifier(QualifierType.PLUS, latestInterval, QualifierUnit.YEARS));	
	}

	@Override
	public void exitPreciseYearRange(PreciseYearRangeContext ctx) {
		if (ctx.exception != null) return;
		
		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();
		
		result.setEarliestSingleDate(earliestDate);
		result.setLatestDate(latestDate);
	}

	@Override
	public void exitPreciseMonthRange(PreciseMonthRangeContext ctx) {
		if (ctx.exception != null) return;
		
		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();
		
		result.setEarliestSingleDate(earliestDate);
		result.setLatestDate(latestDate);
		
		System.out.println(result.toString());
	}

	@Override
	public void exitPreciseMonth(PreciseMonthContext ctx) {
		if (ctx.exception != null) return;

		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();
		
		result.setEarliestSingleDate(earliestDate);
		result.setLatestDate(latestDate);
	}

	@Override
	public void exitPreciseDate(PreciseDateContext ctx) {
		if (ctx.exception != null) return;

		Date date = (Date) stack.pop();

		result.setEarliestSingleDate(date);
	}
	
	@Override
	public void exitPreciseDateRange(PreciseDateRangeContext ctx) {
		if (ctx.exception != null) return;

		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();

		result.setEarliestSingleDate(earliestDate);
		result.setLatestDate(latestDate);
	}

	@Override
	public void exitSmallDateRangeOnly(SmallDateRangeOnlyContext ctx) {
		if (ctx.exception != null) return;

		result.setLatestDate((Date) stack.pop());
		result.setEarliestSingleDate((Date) stack.pop());
	}

	@Override
	public void exitYearRange(YearRangeContext ctx) {
		if (ctx.exception != null) return;

		Date secondYearEndDate = (Date) stack.pop();
		stack.pop(); // secondYearStartDate
		stack.pop(); // firstYearEndDate
		Date firstYearStartDate = (Date) stack.pop();

		// If no era was explicitly specified for the first year,
		// make it inherit the era of the second year.

		if (firstYearStartDate.getEra() == null) {
			firstYearStartDate.setEra(secondYearEndDate.getEra());
		}
		
		stack.push(firstYearStartDate);
		stack.push(secondYearEndDate);
	}

	@Override
	public void exitMonthRange(MonthRangeContext ctx) {
		if (ctx.exception != null) return;

		Date secondMonthEndDate = (Date) stack.pop();
		stack.pop(); // secondMonthStartDate
		stack.pop(); // firstMonthEndDate
		Date firstMonthStartDate = (Date) stack.pop();

		// If no era was explicitly specified for the first month,
		// make it inherit the era of the second month.

		if (firstMonthStartDate.getEra() == null) {
			firstMonthStartDate.setEra(secondMonthEndDate.getEra());
		}
		
		stack.push(firstMonthStartDate);
		stack.push(secondMonthEndDate);
	}

	@Override
	public void exitDateRange(DateRangeContext ctx) {
		if (ctx.exception != null) return;

		Date latestDate = (Date) stack.pop();
		Date earliestDate = (Date) stack.pop();
	
		// If no era was explicitly specified for the first year,
		// make it inherit the era of the second year.

		if (earliestDate.getEra() == null) {
			earliestDate.setEra(latestDate.getEra());
		}
		
		stack.push(earliestDate);
		stack.push(latestDate);
	}

	@Override
	public void exitMonthInYearRange(MonthInYearRangeContext ctx) {
		if (ctx.exception != null) return;

		Integer year = (Integer) stack.pop();
		Integer numMonthEnd = (Integer) stack.pop();
		Integer numMonthStart = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonthStart, 1));
		stack.push(new Date(year, numMonthEnd, DateUtils.getDaysInMonth(numMonthEnd, year)));		
	}
	
	@Override
	public void exitNumDayInMonthRange(NumDayInMonthRangeContext ctx) {
		if (ctx.exception != null) return;

		Integer year = (Integer) stack.pop();
		Integer dayOfMonthEnd = (Integer) stack.pop();
		Integer dayOfMonthStart = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonth, dayOfMonthStart));
		stack.push(new Date(year, numMonth, dayOfMonthEnd));		
	}

	@Override
	public void exitStrDayInMonthRange(StrDayInMonthRangeContext ctx) {
		if (ctx.exception != null) return;

		Integer year = (Integer) stack.pop();
		Integer dayOfMonthEnd = (Integer) stack.pop();
		Integer dayOfMonthStart = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonth, dayOfMonthStart));
		stack.push(new Date(year, numMonth, dayOfMonthEnd));		
	}
	
	@Override
	public void exitStrDate(StrDateContext ctx) {
		if (ctx.exception != null) return;

		Era era = (Era) stack.pop();
		Integer year = (Integer) stack.pop();
		Integer dayOfMonth = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonth, dayOfMonth).withEra(era));
	}

	@Override
	public void exitInvStrDate(InvStrDateContext ctx) {
		if (ctx.exception != null) return;

		Integer dayOfMonth = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		Integer year = (Integer) stack.pop();
		Era era = (Era) stack.pop();
		
		stack.push(new Date(year, numMonth, dayOfMonth).withEra(era));
	}
	
	@Override
	public void exitNumDate(NumDateContext ctx) {
		if (ctx.exception != null) return;

		Era era = (Era) stack.pop();
		Integer year = (Integer) stack.pop();
		Integer dayOfMonth = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonth, dayOfMonth).withEra(era));
	}

	@Override
	public void exitMonthYear(MonthYearContext ctx) {
		if (ctx.exception != null) return;

		Era era = (Era) stack.pop();
		Integer year = (Integer) stack.pop();
		Integer numMonth = (Integer) stack.pop();
		
		stack.push(new Date(year, numMonth, 1).withEra(era));
		stack.push(new Date(year, numMonth, DateUtils.getDaysInMonth(numMonth, year)).withEra(era));		
	}
	
	@Override
	public void exitInvMonthYear(InvMonthYearContext ctx) {
		if (ctx.exception != null) return;

		Integer numMonth = (Integer) stack.pop();
		Integer year = (Integer) stack.pop();
		Era era = (Era) stack.pop();
		
		stack.push(new Date(year, numMonth, 1).withEra(era));
		stack.push(new Date(year, numMonth, DateUtils.getDaysInMonth(numMonth, year)).withEra(era));		
	}

	@Override
	public void exitYear(YearContext ctx) {
		if (ctx.exception != null) return;

		Era era = (Era) stack.pop();
		Integer year = (Integer) stack.pop();
		
		stack.push(new Date(year, FIRST_MONTH, FIRST_DAY_OF_FIRST_MONTH).withEra(era));
		stack.push(new Date(year, LAST_MONTH, LAST_DAY_OF_LAST_MONTH).withEra(era));
	}

	@Override
	public void exitNumYear(NumYearContext ctx) {
		if (ctx.exception != null) return;

		stack.push(new Integer(ctx.NUMBER().getText()));
	}

	@Override
	public void exitNumMonth(NumMonthContext ctx) {
		if (ctx.exception != null) return;

		stack.push(new Integer(ctx.NUMBER().getText()));
	}
	
	@Override
	public void exitStrMonth(StrMonthContext ctx) {
		if (ctx.exception != null) return;
		
		TerminalNode monthNode = ctx.MONTH();
		
		if (monthNode == null) {
			monthNode = ctx.SHORTMONTH();
		}
		
		String monthStr = monthNode.getText();
		
		if (monthStr.equals("sept")) {
			monthStr = "sep";
		}

		stack.push(DateUtils.getMonthByName(monthStr));
	}
	
	@Override
	public void exitEra(EraContext ctx) {
		if (ctx.exception != null) return;

		Era era = null;
		
		if (ctx.BC() != null) {
			era = Era.BCE;
		}
		else if (ctx.AD() != null) {
			era = Era.CE;
		}
		
		stack.push(era);
	}

	@Override
	public void exitNumDayOfMonth(NumDayOfMonthContext ctx) {
		if (ctx.exception != null) return;

		stack.push(new Integer(ctx.NUMBER().getText()));
	}
	
	protected String getErrorMessage(RecognitionException re) {
		String message = "";
		
		Parser recognizer = (Parser) re.getRecognizer();
		TokenStream tokens = recognizer.getInputStream();
		
		if (re instanceof NoViableAltException) {
			NoViableAltException e = (NoViableAltException) re;
			Token startToken = e.getStartToken();
			String input = (startToken.getType() == Token.EOF ) ? "end of text" : quote(tokens.getText(startToken, e.getOffendingToken()));
				
			message = "no viable date format found at " + input;
		}
		else if (re instanceof InputMismatchException) {
			InputMismatchException e = (InputMismatchException) re;
			message = "did not expect " + getTokenDisplayString(e.getOffendingToken()) + " while looking for " +
			          e.getExpectedTokens().toString(recognizer.getTokenNames());
		}
		else if (re instanceof FailedPredicateException) {
			FailedPredicateException e = (FailedPredicateException) re;
            String ruleName = recognizer.getRuleNames()[recognizer.getContext().getRuleIndex()];
            
            message = "failed predicate " + ruleName + ": " + e.getMessage();
		}
		
		return message;
	}
	
	protected String quote(String text) {
		return "'" + text + "'";
	}
	
    protected String getTokenDisplayString(Token token) {
    	String string;
    	
        if (token == null) {
        	string = "[no token]";
        }
        else {
	        String text = token.getText();
	        
	        if (text == null) {
	        	if (token.getType() == Token.EOF ) {
	        		string = "end of text";
	            }
	            else {
	                string = "[" + token.getType() + "]";
	            }
	        }
	        else {
	        	string = quote(text);
	        }
        }
        
        return string;
    }

	public static void main(String[] args) {
		StructuredDateEvaluator evaluator = new ANTLRStructuredDateEvaluator();
		
		for (String displayDate : args) {
			try {
				StructuredDate structuredDate = evaluator.evaluate(displayDate);
				System.out.println(structuredDate.toString());
			} catch (StructuredDateFormatException e) {
				e.printStackTrace();
			}
		}
	}
}