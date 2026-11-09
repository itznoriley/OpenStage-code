package net.openstage;

import net.openstage.core.animation.TextCard;
import net.openstage.core.animation.TextTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextTrack Adversarial QA and Time Range Tests")
class TextTrackTest {
    @Test
    @DisplayName("Empty text track returns empty list for any time")
    void testEmptyTextTrack() {
        TextTrack track = new TextTrack();
        assertTrue(track.getCards().isEmpty());
        List<TextCard> atZero = track.getActiveCardsAt(0.0f);
        assertNotNull(atZero);
        assertTrue(atZero.isEmpty());
        List<TextCard> atNegative = track.getActiveCardsAt(-10.0f);
        assertTrue(atNegative.isEmpty());
        List<TextCard> atPositive = track.getActiveCardsAt(100.0f);
        assertTrue(atPositive.isEmpty());
        List<TextCard> atNan = track.getActiveCardsAt(Float.NaN);
        assertTrue(atNan.isEmpty());
    }

    @Test
    @DisplayName("Boundary edges: exact startTime and exact endTime are inclusive")
    void testBoundaryEdges() {
        TextTrack track = new TextTrack();
        TextCard card = new TextCard("card1", 2.0f, 5.0f, "Boundary Test");
        track.addCard(card);

        assertTrue(track.getActiveCardsAt(1.999f).isEmpty());
        assertEquals(1, track.getActiveCardsAt(2.0f).size());
        assertEquals(card, track.getActiveCardsAt(2.0f).get(0));

        assertEquals(1, track.getActiveCardsAt(3.5f).size());
        assertEquals(card, track.getActiveCardsAt(3.5f).get(0));

        assertEquals(1, track.getActiveCardsAt(5.0f).size());
        assertEquals(card, track.getActiveCardsAt(5.0f).get(0));
        assertTrue(track.getActiveCardsAt(5.001f).isEmpty());
    }

    @Test
    @DisplayName("Overlapping time ranges correctly return all active cards at each time point")
    void testOverlappingTimeRanges() {
        TextTrack track = new TextTrack();
        TextCard cardA = new TextCard("A", 1.0f, 4.0f, "Card A");
        TextCard cardB = new TextCard("B", 2.5f, 6.0f, "Card B");
        TextCard cardC = new TextCard("C", 5.0f, 8.0f, "Card C");

        track.addCard(cardB);
        track.addCard(cardA);
        track.addCard(cardC);

        assertEquals("A", track.getCards().get(0).getId());
        assertEquals("B", track.getCards().get(1).getId());
        assertEquals("C", track.getCards().get(2).getId());

        assertTrue(track.getActiveCardsAt(0.5f).isEmpty());
        List<TextCard> at1 = track.getActiveCardsAt(1.0f);
        assertEquals(1, at1.size());
        assertEquals("A", at1.get(0).getId());
        List<TextCard> at2 = track.getActiveCardsAt(2.0f);
        assertEquals(1, at2.size());
        assertEquals("A", at2.get(0).getId());
        List<TextCard> at3 = track.getActiveCardsAt(3.0f);
        assertEquals(2, at3.size());
        assertTrue(at3.contains(cardA));
        assertTrue(at3.contains(cardB));
        List<TextCard> at4 = track.getActiveCardsAt(4.0f);
        assertEquals(2, at4.size());
        assertTrue(at4.contains(cardA));
        assertTrue(at4.contains(cardB));
        List<TextCard> at45 = track.getActiveCardsAt(4.5f);
        assertEquals(1, at45.size());
        assertEquals("B", at45.get(0).getId());
        List<TextCard> at55 = track.getActiveCardsAt(5.5f);
        assertEquals(2, at55.size());
        assertTrue(at55.contains(cardB));
        assertTrue(at55.contains(cardC));
        List<TextCard> at6 = track.getActiveCardsAt(6.0f);
        assertEquals(2, at6.size());
        assertTrue(at6.contains(cardB));
        assertTrue(at6.contains(cardC));
        List<TextCard> at7 = track.getActiveCardsAt(7.0f);
        assertEquals(1, at7.size());
        assertEquals("C", at7.get(0).getId());
        List<TextCard> at8 = track.getActiveCardsAt(8.0f);
        assertEquals(1, at8.size());
        assertEquals("C", at8.get(0).getId());

        assertTrue(track.getActiveCardsAt(8.001f).isEmpty());
    }

    @Test
    @DisplayName("Negative time: standard cards return empty, negative range cards evaluate accurately")
    void testNegativeTime() {
        TextTrack track = new TextTrack();
        TextCard standardCard = new TextCard("standard", 1.0f, 3.0f, "Standard");
        TextCard negativeCard = new TextCard("negative", -5.0f, -1.0f, "Negative Range");

        track.addCard(standardCard);
        track.addCard(negativeCard);

        assertTrue(track.getActiveCardsAt(-6.0f).isEmpty());
        List<TextCard> atMinus5 = track.getActiveCardsAt(-5.0f);
        assertEquals(1, atMinus5.size());
        assertEquals("negative", atMinus5.get(0).getId());
        List<TextCard> atMinus3 = track.getActiveCardsAt(-3.0f);
        assertEquals(1, atMinus3.size());
        assertEquals("negative", atMinus3.get(0).getId());
        List<TextCard> atMinus1 = track.getActiveCardsAt(-1.0f);
        assertEquals(1, atMinus1.size());
        assertEquals("negative", atMinus1.get(0).getId());

        assertTrue(track.getActiveCardsAt(-0.5f).isEmpty());
        assertTrue(track.getActiveCardsAt(0.0f).isEmpty());
        List<TextCard> at2 = track.getActiveCardsAt(2.0f);
        assertEquals(1, at2.size());
        assertEquals("standard", at2.get(0).getId());
    }

    @Test
    @DisplayName("Adding, removing, and clearing cards in TextTrack")
    void testAddRemoveClear() {
        TextTrack track = new TextTrack();
        track.addCard(null);
        assertTrue(track.getCards().isEmpty());
        TextCard card1 = new TextCard("c1", 0.0f, 1.0f, "First");
        TextCard card2 = new TextCard("c2", 1.0f, 2.0f, "Second");
        track.addCard(card1);
        track.addCard(card2);
        assertEquals(2, track.getCards().size());

        assertDoesNotThrow(() -> track.removeCard(null));
        assertDoesNotThrow(() -> track.removeCard("unknown"));
        assertEquals(2, track.getCards().size());

        track.removeCard("c1");
        assertEquals(1, track.getCards().size());
        assertEquals("c2", track.getCards().get(0).getId());

        track.clear();
        assertTrue(track.getCards().isEmpty());
    }
}

