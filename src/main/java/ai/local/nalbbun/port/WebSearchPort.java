package ai.local.nalbbun.port;

public interface WebSearchPort {

    String search(String query);

    String fetch(String url);

    default String providerName() {
        return "unknown";
    }
}
