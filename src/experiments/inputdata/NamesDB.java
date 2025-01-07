package experiments.inputdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class NamesDB implements DomainsDB {

    private List<String> nameEntries;

    public NamesDB(List<DomainEntry> domainsList) {
        this.nameEntries = new ArrayList<>();
        for (DomainEntry d : domainsList) {
            nameEntries.add(d.getName());
        }
    }

    public NamesDB() {
        this.nameEntries = new ArrayList<>();
        Collections.addAll(nameEntries, "youtube.com", "facebook.com", "wikipedia.org", "reddit.com", "instagram.com",
                "tiktok.com", "pinterest.com", "quora.com", "amazon.com", "linkedin.com",
                "twitter.com", "google.com", "ebay.com", "apple.com", "etsy.com");
    }

    @Override
    public String getRandomEntry() {
        int randomIndex = (new Random()).nextInt(0, nameEntries.size());
        return nameEntries.get(randomIndex);
    }

    @Override
    public List<String> getRandomEntries(int n) {
        List<String> randomEntries = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            randomEntries.add(getRandomEntry());
        }

        return randomEntries;
    }
}
