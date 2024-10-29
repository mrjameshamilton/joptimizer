import org.junit.jupiter.api.Test;
import java.lang.classfile.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OptimizationTest {
    
    @Test
    void shouldRemoveAllNops() {
        // Create a list of instructions that mimics what we're seeing
        List<CodeElement> elements = List.of(
            new Label(0), // Some label
            Instruction.NOP,  // First NOP
            new LoadInstruction(Opcode.ALOAD_0, 0),  // Load
            Instruction.NOP,  // Another NOP
            new StoreInstruction(Opcode.ASTORE, 3),  // Store
            new Label(1),  // Another label
            Instruction.NOP   // Third NOP
        );
        
        // Create code attribute mock
        CodeAttribute code = new CodeAttribute() {
            @Override
            public Stream<CodeElement> elementStream() {
                return elements.stream();
            }
            
            // Implement other required methods...
            @Override
            public List<CodeElement> elementList() {
                return elements;
            }
        };
        
        // Create builder to capture output
        List<CodeElement> output = new ArrayList<>();
        CodeBuilder builder = new CodeBuilder() {
            @Override
            public void with(CodeElement element) {
                output.add(element);
            }
            // Implement other required methods...
        };
        
        // Apply optimization
        optimize(null, code, builder);
        
        // Verify no NOPs in output
        assertFalse(output.stream()
            .anyMatch(e -> e instanceof Instruction i && i.opcode() == Opcode.NOP),
            "Output should not contain any NOPs");
            
        // Verify other instructions preserved
        assertTrue(output.stream()
            .anyMatch(e -> e instanceof LoadInstruction),
            "Output should contain LOAD instruction");
        assertTrue(output.stream()
            .anyMatch(e -> e instanceof StoreInstruction),
            "Output should contain STORE instruction");
        assertEquals(2, output.stream()
            .filter(e -> e instanceof Label)
            .count(),
            "Output should contain both labels");
    }
    
    @Test
    void shouldRemoveConsecutiveNops() {
        List<CodeElement> elements = List.of(
            new Label(0),
            Instruction.NOP,
            Instruction.NOP,  // Consecutive NOPs
            new StoreInstruction(Opcode.ASTORE, 3),
            new Label(1)
        );
        
        // Similar test setup as above...
        
        assertFalse(output.stream()
            .anyMatch(e -> e instanceof Instruction i && i.opcode() == Opcode.NOP),
            "Output should not contain any NOPs");
    }
    
    @Test
    void shouldHandleNopsAroundLabels() {
        List<CodeElement> elements = List.of(
            Instruction.NOP,
            new Label(0),  // NOP before label
            new StoreInstruction(Opcode.ASTORE, 3),
            new Label(1),
            Instruction.NOP  // NOP after label
        );
        
        // Similar test setup as above...
        
        assertFalse(output.stream()
            .anyMatch(e -> e instanceof Instruction i && i.opcode() == Opcode.NOP),
            "Output should not contain any NOPs");
    }
    
    @Test
    void shouldPreserveInstructionOrder() {
        LoadInstruction load = new LoadInstruction(Opcode.ALOAD_0, 0);
        StoreInstruction store = new StoreInstruction(Opcode.ASTORE, 3);
        Label label1 = new Label(0);
        Label label2 = new Label(1);
        
        List<CodeElement> elements = List.of(
            label1,
            Instruction.NOP,
            load,
            Instruction.NOP,
            store,
            label2
        );
        
        // Similar test setup as above...
        
        // Verify order preserved (excluding NOPs)
        List<CodeElement> expectedOrder = List.of(label1, load, store, label2);
        assertEquals(expectedOrder, output,
            "Instructions should maintain their relative order after NOP removal");
    }
}