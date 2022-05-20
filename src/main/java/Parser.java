import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class Parser {
    private final List<Token> tokens;
    private Token currenToken;
    private ListIterator<Token> it;
    private final ListIterator<Token> iterator;
    static List<Expression> expressions = new LinkedList<>();


    public Parser(List<Token> tokens) { // конструктор парсера, принимающий лист с токенами от лексера
        this.tokens = tokens;
        iterator = tokens.listIterator(); //присваиваем первому итератору лист с токенами
    }

    public void parser() {
        it = tokens.listIterator(); //присваиваем второму итератору лист с токенами
        while (iterator.hasNext()) {
            try {
                expr(false); //вызов основного нетерминала expr
            } catch (ParseException pe) {
                System.out.println(pe.getMessage() + pe.getErrorOffset());
                return;
            }
        }
    }


// _____ПРОВЕРКА_НЕТЕРМИНАЛОВ____________________________________________________

    protected void expr (boolean isCalled) throws ParseException {
        if (assign() || while_() || if_()) {
            if (isCalled) return;
            StringBuffer buffer = new StringBuffer();

            while (true) { //добавление полученного выражения в list
                buffer.append(it.next().getValue());
                    //Записываем токены в буфер пока вспомогательный итератор (it) не сравняется с основным (iterator), т.е. достигнет конца одного из expression
                    if (it.nextIndex() == iterator.nextIndex()) {
                        break;
                }
            }

            expressions.add(new Expression(buffer));
        } else {
            throw new ParseException("ASSIGN or WHILE or IF expected, but ", iterator.nextIndex());
        }
    }

    protected boolean assign() throws ParseException { //основной нетерминал assign
        if (var()) {
            if (assign_op()) {
                return expr_value();
            } else {
                throw new ParseException("ASSIGN_OP expected, but", iterator.nextIndex());
            }
        } else return false;
    }

    protected boolean while_() throws ParseException { //основной нетерминал while
        if (while_keyword()) {
            if (condition_while()) {
                return body_while();
            } else {
                throw new ParseException("CONDITION expected, but", iterator.nextIndex());
            }
        } else return false;
    }

    protected boolean inf_Parenthesis_val() throws ParseException{ //нетерминал для случаев типа (...) + 100
        if (left_bracket()) {
            if (expr_value() && right_bracket()) {
                while (iterator.hasNext() && op()) expr_value(); //(OP expr_value)*
                return true;
            }
        }
        return false;
    }

    protected boolean val_inf_Parenthesis() throws ParseException { //нетерминал для случаев типа 100 + (...)
        boolean hasChain = true;
        if (value()) {
            while (iterator.hasNext() && op()) hasChain = (value() || inf_Parenthesis_val()); //(OP (value | inf_Parenthesis_val) )*
            return hasChain;
        } else return false;
    }

    protected boolean expr_value() throws ParseException { //нетерминал для всех выражений в assign
        if (inf_Parenthesis_val() || val_inf_Parenthesis()) {
            return true;
        } else {
            throw new ParseException("VAR or DIGIT expected, but", iterator.nextIndex());
        }
    }

    protected boolean if_() throws ParseException {
        if (if_keyword()) {
            if (left_bracket() && compare() && right_bracket()) {
                if (start_while()) {
                    expr(true);
                    while (finish_while()) { //ожидание конца тела условия if, считывание множества терминалов до '}'
                        expr(true);
                    }

                    if (else_keyword()) {
                        if (start_while()) {
                            expr(true);
                            while (finish_while()) { //ожидание конца тела условия else, считывание множества терминалов до '}'
                                expr(true);
                            }
                            return true;
                        }
                        throw new ParseException("START_WHILE expected, but", iterator.nextIndex());
                    } else return true;
                }
            }
            throw new ParseException("IF or BRACKET or COMPARE expected, but", iterator.nextIndex());
        } else return false;
    }

    /*protected boolean op_value() throws ParseException { //старый метод для нетерминала assign
        if (op()) {
            if (value()) {
                if (iterator.hasNext() && op()) {
                    prev();
                    return op_value(); //рекурсивный вызов функции для случаев с большим количеством операций
                } else return true;
            } else {
                throw new ParseException("VAR or DIGIT expected, but", iterator.nextIndex());
            }
        } else {
            throw new ParseException("OP expected, but", iterator.nextIndex());
        }
    }*/

    protected boolean value() {
        return var() || digit();
    }

    protected boolean condition_while() { //нетерминал тела условия цикла while
        return left_bracket() && compare() && right_bracket();
    }

    protected boolean compare() { //нетерминал условия цикла while
        return value() && op_bool() && value();
    }

    protected boolean body_while() throws ParseException { //нетерминал тела цикла while
        if (start_while()) {
            expr(true);
            while (finish_while()/*!iterator.next().getType().equals("FINISH_BODY")*/) { //ожидание конца тела цикла while, считывание множества терминалов до '}'
                expr(true);
            }
            return true;
        } else {
            throw new ParseException("START WHILE expected, but", iterator.nextIndex());
        }
    }


//  _____ПРОВЕРКА_ЛЕКСЕМ/ТЕРМИНАЛОВ________________________________________________________

    protected boolean var() {
        return checkToken("VAR");
    }

    protected boolean assign_op() {
       return checkToken("ASSIGN_OP");
    }

    protected boolean digit() {
        return checkToken("DIGIT");
    }

    protected boolean op() {
        return checkToken("OP");
    }

    protected boolean while_keyword() {
        return checkToken("WHILE_KEYWORD");
    }

    protected boolean left_bracket() {
        return checkToken("L_BRACKET");
    }

    protected boolean right_bracket() {
        return checkToken("R_BRACKET");
    }

    protected boolean op_bool() {
        return checkToken("OP_BOOL");
    }

    protected boolean var_bool(int i) {
        return checkToken("BOOL_KEYWORD");
    }

    protected boolean start_while() {
        return checkToken("START_BODY");
    }

    protected boolean finish_while() {
        return !checkToken("FINISH_BODY");
    }

    protected boolean if_keyword() {
        return checkToken("IF_KEYWORD");
    }

    protected boolean else_keyword() {
        return checkToken("ELSE_KEYWORD");
    }

    protected void next() {
        currenToken = iterator.next(); //двигаем итератор по листу токенов на один элемент вперед
    }

    protected void prev() {
        currenToken = iterator.previous(); //двигаем итератор по листу токенов на один элемент назад
    }

    protected boolean checkToken(String name) {
        next();
        boolean result = this.currenToken.getType().equals(name); //проверяем, равен ли текущий токен тому, что передали в переменную name
        if (!result) { //если токен не совпал, то двигаем итератор назад
            prev();
        }
        return result;
    }
}
