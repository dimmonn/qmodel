package com.research.qmodel.service.findbugs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to verify getChangedLineNumbers implementation
 */
@ExtendWith(MockitoExtension.class)
class BasicBugFinderDebugTest {
    
    @Mock private CommitRepository commitRepository;
    @Mock private ProjectIssueRepository projectIssueRepository;
    @InjectMocks private BasicBugFinder basicBugFinder;
    
    @Test
    void debugPatchParsing() throws Exception {
        String patch = "@@ -5,10 +7,12 @@\n" +
                " context line\n" +
                "+added line 1\n" +
                "+added line 2\n" +
                " context line\n" +
                "-deleted line\n" +
                "+added line 3\n";
        
        Method method = BasicBugFinder.class.getDeclaredMethod("getChangedLineNumbers", String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Set<Integer> result = (Set<Integer>) method.invoke(basicBugFinder, patch);
        
        System.out.println("Patch: " + patch.replace("\n", "\\n"));
        System.out.println("Result: " + result);
        System.out.println("Contains 8: " + result.contains(8));
        System.out.println("Contains 9: " + result.contains(9));
        System.out.println("Contains 11: " + result.contains(11));
        System.out.println("Contains 12: " + result.contains(12));
        
        assertTrue(result.contains(8), "Should contain 8");
        assertTrue(result.contains(9), "Should contain 9");
        assertTrue(result.contains(11), "Should contain 11");
    }
    
    @Test
    void debugDeletionOnlyPatch() throws Exception {
        String patch = "@@ -5,10 +5,8 @@\n" +
                " context\n" +
                "-deleted line 1\n" +
                "-deleted line 2\n" +
                " context\n";
        
        Method method = BasicBugFinder.class.getDeclaredMethod("getChangedLineNumbers", String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Set<Integer> result = (Set<Integer>) method.invoke(basicBugFinder, patch);
        
        System.out.println("Deletion-only patch result: " + result);
        System.out.println("Is empty: " + result.isEmpty());
        
        assertTrue(result.isEmpty(), "Deletion-only patch should have no added lines");
    }
}

