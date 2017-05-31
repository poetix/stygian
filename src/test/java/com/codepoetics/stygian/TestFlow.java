package com.codepoetics.stygian;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static com.codepoetics.stygian.java.JavaFlow.*;
import static org.junit.Assert.assertEquals;

public class TestFlow {

    private static final Logger LOGGER = Logger.getLogger(TestFlow.class.getName());

    @Test
    public void simpleflow() throws ExecutionException, InterruptedException {
        Flow<String, String> selectGreeting = createSelectGreetingFlow();
        Flow<String, String> selectCase = createSelectCaseFlow();
        Flow<String, String> emphasiseAndPrint = createEmphasiseAndPrintFlow();

        Flow<String, String> greet = selectGreeting
                .then(selectCase)
                .then(emphasiseAndPrint);

        System.out.println(prettyPrint(greet));

        assertEquals("Greetings, sire!", runLogging(greet, "King Rollo", LOGGER::info).get());
        assertEquals("Greetings, ma'am!", runLogging(greet, "Queen Quenfrith", LOGGER::info).get());
        assertEquals("HELLO FLOKI!", runLogging(greet, "Floki", LOGGER::info).get());
    }

    private Flow<String, String> createSelectCaseFlow() {
        Flow<String, String> leaveLowercase = flow("Leave lowercase", (String name) -> name);
        Condition<String> isHardOfHearing = condition("is hard of hearing", name -> name.contains("Floki"));
        Flow<String, String> makeUppercase = flow("Uppercase", String::toUpperCase);

        return leaveLowercase.orIf(isHardOfHearing, makeUppercase);
    }

    private Flow<String, String> createEmphasiseAndPrintFlow() {
        Flow<String, String> emphasise = flow("Add exclamation mark", name -> name + "!");
        Flow<String, String> printToConsole = flow("Print greeting to console", name -> {
            System.out.println(name);
            return name;
        });

        return emphasise.then(printToConsole);
    }

    private Flow<String, String> createSelectGreetingFlow() {
        Flow<String, String> greetCommoner = flow("Say hello", name -> "Hello " + name);
        Condition<String> isKing = condition("is king", name -> name.contains("King"));
        Flow<String, String> greetKing = flow("Greet the king", name -> "Greetings, sire");
        Condition<String> isQueen = condition("is queen", name -> name.contains("Queen"));
        Flow<String, String> greetQueen = flow("Greet the queen", name -> "Greetings, ma'am");

        return greetCommoner
            .orIf(isKing, greetKing)
            .orIf(isQueen, greetQueen);
    }

}
