# Validation and Formatting
## util package
*Turns raw text from the screen into checked values: text, optionalText, number, positiveNumber, nonNegativeNumber, positiveInt, date, choice. Each throws a ValidationException with a readable message, so screens never deal with NumberFormatException.*

#### final class Fmt
A static utility class (no subclasses) responsible for turning raw Java numbers and dates into consistent, human-readable strings across all screens, stat cards, and tables. 
Formatting: money (PHP 1,250.50), date, dateTime, round2 (rounds to centavos) and daysPhrase ("3 days", "Today", "Expired 2 days ago").

#### final class Input
A static utility class (no subclasses) that converts raw string inputs and dropdown selections from UI forms (FormDialog, SellScreen) into validated Java values, throwing a ValidationException with a clear field-specific message whenever input is blank, malformed, or out of range. 
