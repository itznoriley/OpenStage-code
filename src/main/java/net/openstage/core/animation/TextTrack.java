package net.openstage.core.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TextTrack {
    private final List<TextCard> cards = new ArrayList<>();

    public void addCard(TextCard card) {
        if (card != null) {
            cards.add(card);
            sortCards();
        }
    }

    public void removeCard(String id) {
        if (id != null) {
            cards.removeIf(c -> c != null && id.equals(c.getId()));
        }
    }

    public void removeCard(TextCard card) {
        if (card != null) {
            cards.remove(card);
        }
    }

    public List<TextCard> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public List<TextCard> getActiveCardsAt(float time) {
        List<TextCard> active = new ArrayList<>();
        for (TextCard card : cards) {
            if (card != null && card.isActiveAt(time)) {
                active.add(card);
            }
        }
        return active;
    }

    public void clear() {
        cards.clear();
    }

    private void sortCards() {
        cards.sort(Comparator.comparingDouble(TextCard::getStartTime));
    }
}
