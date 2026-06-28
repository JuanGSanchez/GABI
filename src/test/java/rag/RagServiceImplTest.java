package rag;

import core.AnswerWithSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import tables.Book;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RagServiceImpl} — tests the ingest and ask pipeline
 * using Mockito mocks for {@link SimpleVectorStore} and {@link ChatClient.Builder}.
 *
 * <p>No live Ollama, no OpenAI/Anthropic key required. All AI interactions are mocked.
 */
@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private SimpleVectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private RagServiceImpl ragService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ragService = new RagServiceImpl(vectorStore, chatClientBuilder);
    }

    // =========================================================================
    // ingest
    // =========================================================================

    @Test
    @DisplayName("ingest() projects each book to a Document and calls vectorStore.add()")
    void ingest_callsVectorStoreAdd() {
        List<Book> books = List.of(
                new Book(1, "Dune", "Herbert", false),
                new Book(2, "Foundation", "Asimov", true)
        );

        ragService.ingest(books);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> addedDocs = captor.getValue();
        assertThat(addedDocs).isNotEmpty();

        // The documents should contain text about the books
        String combinedText = addedDocs.stream()
                .map(Document::getText)
                .reduce("", String::concat);
        assertThat(combinedText)
                .containsIgnoringCase("Dune")
                .containsIgnoringCase("Herbert");
    }

    @Test
    @DisplayName("ingest() with empty list does NOT call vectorStore.add()")
    void ingest_emptyBooks_doesNotCallAdd() {
        ragService.ingest(List.of());
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("ingest() with null does NOT call vectorStore.add()")
    void ingest_null_doesNotCallAdd() {
        ragService.ingest(null);
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("ingest() document includes book status (lent vs available)")
    void ingest_documentIncludesLentStatus() {
        Book lentBook = new Book(1, "Lent Book", "Author A", true);
        Book availBook = new Book(2, "Free Book", "Author B", false);

        ragService.ingest(List.of(lentBook, availBook));

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        String combinedText = captor.getValue().stream()
                .map(Document::getText)
                .reduce("", String::concat);

        assertThat(combinedText)
                .containsIgnoringCase("lent")
                .containsIgnoringCase("available");
    }

    @Test
    @DisplayName("ingest() sets metadata with source=library-catalogue")
    void ingest_documentHasMetadata() {
        ragService.ingest(List.of(new Book(1, "Dune", "Herbert", false)));

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        // At least one document should have the library-catalogue source metadata
        boolean hasSource = captor.getValue().stream()
                .anyMatch(doc -> "library-catalogue".equals(doc.getMetadata().get("source")));
        assertThat(hasSource).isTrue();
    }

    // =========================================================================
    // ask
    // =========================================================================

    @Test
    @DisplayName("ask() invokes chatClient and returns the answer with sources")
    void ask_returnsChatResponse() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Dune is available.");

        // similarity search for sources
        Document sourceDoc = Document.builder().text("Book ID: 1 | Title: Dune").build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(sourceDoc));

        // Act
        AnswerWithSources result = ragService.ask("Is Dune available?");

        // Assert
        assertThat(result.answer()).isEqualTo("Dune is available.");
        assertThat(result.sources()).containsExactly("Book ID: 1 | Title: Dune");
    }

    @Test
    @DisplayName("ask() handles null response from chatClient gracefully")
    void ask_nullResponse_returnsEmptyAnswer() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(null);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        AnswerWithSources result = ragService.ask("Any question?");
        assertThat(result.answer()).isEmpty();
        assertThat(result.sources()).isEmpty();
    }

    @Test
    @DisplayName("ask() gracefully degrades when chatClient throws an exception")
    void ask_modelError_returnsGracefulDegradation() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Ollama not reachable"));

        AnswerWithSources result = ragService.ask("Any question?");

        assertThat(result.answer()).isNotBlank();
        assertThat(result.answer()).containsIgnoringCase("RAG");
        assertThat(result.sources()).isEmpty();
    }

    @Test
    @DisplayName("ask() returns empty sources when similarity search fails")
    void ask_sourceRetrievalFails_returnsEmptySources() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("An answer.");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("vector store error"));

        AnswerWithSources result = ragService.ask("Any question?");
        assertThat(result.answer()).isEqualTo("An answer.");
        assertThat(result.sources()).isEmpty();
    }
}
