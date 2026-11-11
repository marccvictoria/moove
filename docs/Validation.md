# Validation and Formatting
## util package
*Turns raw text from the screen into checked values: text, optionalText, number, positiveNumber, nonNegativeNumber, positiveInt, date, choice. Each throws a ValidationException with a readable message, so screens never deal with NumberFormatException.*

#### Fmt
Formatting: money (PHP 1,250.50), date, dateTime, round2 (rounds to centavos) and daysPhrase ("3 days", "Today", "Expired 2 days ago").
```text
```
##### subclass