package net.openstage.client.ui;

import net.openstage.client.model.ModelSettings;
import java.util.HashMap;
import java.util.Map;

public class I18n {
    private static final Map<String, String> RU = new HashMap<>();

    static {
        RU.put("File", "Файл");
        RU.put("Edit", "Правка");
        RU.put("View", "Вид");
        RU.put("Settings", "Настройки");
        RU.put("Pose", "Поза");
        RU.put("Model", "Модель");
        RU.put("Preset", "Пресеты");
        RU.put("Presets", "Пресеты");

        RU.put("New Model... (Ctrl+N)", "Новый проект... (Ctrl+N)");
        RU.put("Save Project (Ctrl+S)", "Сохранить проект (Ctrl+S)");
        RU.put("Rename Project...", "Переименовать проект...");
        RU.put("Rename Project", "Переименование проекта");
        RU.put("New Name:", "Новое имя:");
        RU.put("Rename", "Переименовать");
        RU.put("Cancel", "Отмена");
        RU.put("Create New Model / Project", "Создать модель / проект");
        RU.put("Name:", "Имя:");
        RU.put("Static Pose (.ospose.json)", "Статичная поза (.ospose.json)");
        RU.put("Single pose for statues, props, model blocks", "Одна поза для статуй, декораций и блоков");
        RU.put("Animation (.osanim.json)", "Анимация (.osanim.json)");
        RU.put("Full timeline with keyframes & motion playback", "Шкала времени с кадрами и воспроизведением");
        RU.put("Create", "Создать");
        RU.put("Import Model (.geo.json, .bbmodel, .obj)...", "Импорт модели (.geo.json, .bbmodel, .obj)...");
        RU.put("Export Video (.mp4 / PNG)...", "Экспорт видео (.mp4 / PNG)...");
        RU.put("Export Video (mp4 / PNG)...", "Экспорт видео (mp4 / PNG)...");
        RU.put("Open Animations Folder", "Папка анимаций");
        RU.put("Open Skins Folder", "Папка скинов");
        RU.put("Exit Editor (Esc)", "Выйти из редактора (Esc)");

        RU.put("Undo (Ctrl+Z)", "Отменить (Ctrl+Z)");
        RU.put("Redo (Ctrl+Y)", "Повторить (Ctrl+Y)");
        RU.put("Copy Rotation (Ctrl+C)", "Копировать поворот (Ctrl+C)");
        RU.put("Paste Rotation (Ctrl+V)", "Вставить поворот (Ctrl+V)");
        RU.put("Add Keyframe (K)", "Добавить кадр (K)");
        RU.put("Delete Keyframe (Del)", "Удалить кадр (Del)");
        RU.put("Clear Frame Keys", "Очистить кадры");

        RU.put("Toggle Graph (Tab)", "Переключить график (Tab)");
        RU.put("Toggle Camera Frustum", "Показать пирамиду камеры");
        RU.put("Reset Viewport Camera", "Сбросить камеру");
        RU.put("Camera", "Камера");
        RU.put("Camera Mode", "Режим камеры");
        RU.put("🎬 Cutscene", "🎬 Катсцена");
        RU.put("Front View (Num 1)", "Вид спереди (Num 1)");
        RU.put("Side View (Num 3)", "Вид сбоку (Num 3)");
        RU.put("Top View (Num 7)", "Вид сверху (Num 7)");
        RU.put("Isometric (Num 5)", "Изометрия (Num 5)");

        RU.put("Editor Settings", "Настройки редактора");
        RU.put("Language / Язык", "Язык интерфейса");
        RU.put("Theme", "Тема оформления");
        RU.put("Timeline Snapping", "Привязка времени");
        RU.put("Show 3D Grid", "Сетка в окне 3D");
        RU.put("World Background", "Фон из мира");
        RU.put("Ghost / Onion Skin", "Калька");
        RU.put("Ghost Prev/Next Frame", "Калька след./пред. кадра");
        RU.put("BBS Style Selection Bounds", "3D выделение контура (BBS)");
        RU.put("Bendable Limbs (IK)", "Сгибаемые суставы (IK)");
        RU.put("Show Bone Wireframes", "Каркас костей");

        RU.put("Character Configuration", "Настройки персонажа");
        RU.put("Character Settings", "Настройки персонажа");
        RU.put("Layer Visibility", "Видимость слоёв");
        RU.put("Animated Eyes", "Анимированные глаза");
        RU.put("Root Placement", "Позиционирование корня");
        RU.put("Props", "Предметы в руках");
        RU.put("Held Items", "Предметы в руках");
        RU.put("Animation & Joints", "Анимация и суставы");
        RU.put("Предметы в руках (Props)", "Предметы в руках");
        RU.put("Анимация и суставы", "Анимация и суставы");

        RU.put("Model: Steve (Wide)", "Модель: Стив (4px)");
        RU.put("Model: Alex (Slim)", "Модель: Алекс (3px)");
        RU.put("Classic (4px)", "Классика (4px)");
        RU.put("Slim (3px)", "Тонкий (3px)");
        RU.put("Skin", "Скин");
        RU.put("Skin Folder", "Папка скинов");
        RU.put("Папка скинов", "Папка скинов");
        RU.put("Refresh", "Обновить");
        RU.put("Обновить", "Обновить");
        RU.put("Upload Skin (.png)", "Загрузить скин (.png)");
        RU.put("Загрузить скин (.png)", "Загрузить скин (.png)");

        RU.put("Right: [Empty - Click]", "Правая: [Пусто - Нажмите]");
        RU.put("Left: [Empty - Click]", "Левая: [Пусто - Нажмите]");
        RU.put("Правая: [Пусто – Нажмите]", "Правая: [Пусто - Нажмите]");
        RU.put("Левая: [Пусто – Нажмите]", "Левая: [Пусто - Нажмите]");
        RU.put("R Hand: ", "Пр: ");
        RU.put("L Hand: ", "Лев: ");

        RU.put("IK Mode (kinematics)", "Режим IK (кинематика)");
        RU.put("Режим IK (кинематика)", "Режим IK (кинематика)");
        RU.put("Onion Skin", "Калька");
        RU.put("Onion Skin (калька)", "Калька");
        RU.put("Калька (Onion Skin)", "Калька");

        RU.put("Hat", "Шлем");
        RU.put("Jacket", "Куртка");
        RU.put("R Sleeve", "Пр. рукав");
        RU.put("L Sleeve", "Лев. рукав");
        RU.put("R Pants", "Пр. штаны");
        RU.put("L Pants", "Лев. штаны");

        RU.put("Yaw: 0° (+45°)", "Поворот (Yaw): 0° (+45°)");
        RU.put("Scale: 1.0x", "Масштаб (Scale): 1.0x");
        RU.put("Position", "Позиция");
        RU.put("Rotation", "Вращение");
        RU.put("Scale", "Масштаб");

        RU.put("Player", "Игрок");
        RU.put("Recent", "Недавние");
        RU.put("Custom", "Кастомные");
        RU.put("Standard", "Обычные");
        RU.put("With Bones", "С костями");

        RU.put("OpenStage Preferences", "Настройки OpenStage");
        RU.put("UI Color Themes", "Цветовые темы интерфейса");
        RU.put("Dark Slate", "Тёмный сланец");
        RU.put("Midnight", "Полночь");
        RU.put("Charcoal", "Угольный");
        RU.put("Emerald", "Изумруд");
        RU.put("Amber", "Янтарь");
        RU.put("Synthwave", "Синтвейв");
        RU.put("Monokai", "Монокай");
        RU.put("Light Studio", "Светлая студия");
        RU.put("Camera Navigation Mode", "Режим навигации камеры");
        RU.put("Orbit (Mouse)", "Орбита (Мышь)");
        RU.put("Free Fly (WASD)", "Свободный полёт (WASD)");
        RU.put("Blender (MMB)", "Блендер (СКМ)");
        RU.put("Viewport Environment", "Окружение вьюпорта");
        RU.put("Floor Grid", "Сетка пола");
        RU.put("World Scan", "Скан мира");
        RU.put("Gizmo", "3D Гизмо");
        RU.put("UI Animations", "Анимации UI");
        RU.put("Keybindings", "Горячие клавиши");
        RU.put("Play / Pause", "Воспр. / Пауза");
        RU.put("Add Keyframe", "Добавить ключ");
        RU.put("Delete Key", "Удалить ключ");
        RU.put("Toggle Graph", "Редактор кривых");
        RU.put("Camera View", "👁 Вид камеры");
        RU.put("Interface Language", "Язык интерфейса");
        RU.put("Language", "Язык");

        RU.put("Viewport Display", "Отображение вьюпорта");
        RU.put("Ground Grid", "Сетка пола");
        RU.put("In-World View", "Вид в мире");
        RU.put("3D Rotation Gizmo", "3D Гизмо вращения");
        RU.put("Camera: Mouse Orbit", "Камера: Вращение мышью");
        RU.put("Camera: Free Fly (WASD)", "Камера: Свободный полёт (WASD)");
        RU.put("Camera: Blender (MMB)", "Камера: Блендер (СКМ)");
        RU.put("Reset Camera", "Сбросить камеру");

        RU.put("Animation Presets", "Пресеты анимаций");
        RU.put("T-Pose", "Т-поза");
        RU.put("Idle", "Покой (Idle)");
        RU.put("Walk", "Ходьба");
        RU.put("Run", "Бег");
        RU.put("Wave", "Махать рукой");
        RU.put("Sitting", "Сидеть");
        RU.put("Sneak", "Приседание");
        RU.put("Victory", "Победа");
        RU.put("Clap", "Аплодисменты");
        RU.put("Thinking", "Размышление");
        RU.put("Sleeping", "Сон");

        RU.put("Bone: ", "Кость: ");
        RU.put("Rotate", "Вращение");
        RU.put("Move", "Перемещение");
        RU.put("Drag to rotate:", "Тяните для вращения:");
        RU.put("Drag to move position:", "Тяните для смещения:");
        RU.put("Reset Rotations (0°)", "Сбросить вращение (0°)");
        RU.put("Reset Position (0, 0, 0)", "Сбросить позицию (0, 0, 0)");
        RU.put("Easing: ", "Сглаживание: ");
        RU.put("linear", "линейное");
        RU.put("step", "ступенчатое");
        RU.put("ease_in", "ускорение (ease in)");
        RU.put("ease_out", "замедление (ease out)");
        RU.put("ease_in_out", "плавное (ease in-out)");
        RU.put("bezier", "кривая Безье");
        RU.put("Add Text Card", "Добавить текст-карту");
        RU.put("Particles", "Частицы");
        RU.put("eyes (Animated)", "Глаза (Анимированные)");

        RU.put("SCENE ACTORS", "АКТЁРЫ СЦЕНЫ");
        RU.put("Saved Projects", "Сохранённые проекты");
        RU.put("Selected: ", "Выбрано: ");
        RU.put("Zero Pose", "Обнулить позу");
        RU.put("Add Actor", "Добавить актёра");
        RU.put("Create Project", "Создать проект");
        RU.put("Transform", "Трансформ");
        RU.put("Pose", "Поза");
        RU.put("Model", "Модель");
        RU.put("Preset", "Пресеты");
        RU.put("Presets", "Пресеты");
        RU.put("Sitting", "Сидеть");

        RU.put("Text Cards", "Текстовые плашки");
        RU.put("Audio", "Аудио");
        RU.put("♫ Audio", "♫ Аудио");
        RU.put("Tracks", "Дорожки");
        RU.put("No Model Loaded", "Модель не загружена");
        RU.put("Select or create a model to begin", "Выберите или создайте модель для начала");
        RU.put("Create New Model", "Создать новую модель");
        RU.put("Static Pose", "Статичная поза");
        RU.put("Animation", "Анимация");
        RU.put("No active actors", "Нет активных актеров");
        RU.put("No files found", "Файлы не найдены");
        RU.put("Save", "Сохранить");
        RU.put("Cycle Eye Color", "Сменить цвет глаз");
        RU.put("New Text Plate", "Новая плашка");
        RU.put("New Model Plate", "Новая плашка");

        RU.put("Model Palette", "Палитра моделей");
        RU.put("Add Model...", "Добавить модель...");
        RU.put("Models Folder", "Папка моделей");
        RU.put("Load Model", "Выбрать модель");
        RU.put("Search models...", "Поиск моделей...");
        RU.put("Player Models", "Модели игроков");
        RU.put("Custom Models", "Пользовательские модели");
        RU.put("Recent", "Недавние");
        RU.put("Steve (Standard)", "Стив (Стандарт)");
        RU.put("Alex (Standard)", "Алекс (Стандарт)");
        RU.put("Steve (Bends)", "Стив (Кости)");
        RU.put("Alex (Bends)", "Алекс (Кости)");
        RU.put("No custom models found in openstage/models", "В папке openstage/models нет моделей");
        RU.put("None", "Нет");
        RU.put("Load Project", "Загрузить проект");
        RU.put("Duplicate", "Дублировать");
        RU.put("Delete", "Удалить");
        RU.put("Open Folder", "Открыть папку");
        RU.put("Project Actions", "Действия с проектом");
        RU.put("✂ Split", "✂ Разрез");
        RU.put("♫ Audio", "♫ Аудио");
        RU.put("T Text", "T Текст");
    }

    public static String get(String key, ModelSettings.Language lang) {
        if (key == null) return "";
        if (lang == ModelSettings.Language.RU) {
            String val = RU.get(key);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }
        return key;
    }

    public static String get(String key) {
        return get(key, ModelSettings.getGlobalLanguage());
    }

    public static String getBoneDisplayName(String bone, ModelSettings.Language lang) {
        if (bone == null) return "";
        if (lang == ModelSettings.Language.RU) {
            return switch (bone) {
                case "root" -> "Полное тело";
                case "body" -> "Торс";
                case "head" -> "Голова";
                case "right_arm" -> "Прав. рука";
                case "right_forearm" -> "Пр. предплечье";
                case "left_arm" -> "Лев. рука";
                case "left_forearm" -> "Лев. предплечье";
                case "right_leg" -> "Прав. нога";
                case "right_lower_leg" -> "Пр. голень";
                case "left_leg" -> "Лев. нога";
                case "left_lower_leg" -> "Лев. голень";
                case "eyes" -> "Глаза";
                case "camera" -> "Камера";
                default -> bone;
            };
        }
        return switch (bone) {
            case "root" -> "Root";
            case "body" -> "Body";
            case "head" -> "Head";
            case "right_arm" -> "Right Arm";
            case "right_forearm" -> "Right Forearm";
            case "left_arm" -> "Left Arm";
            case "left_forearm" -> "Left Forearm";
            case "right_leg" -> "Right Leg";
            case "right_lower_leg" -> "Right Lower Leg";
            case "left_leg" -> "Left Leg";
            case "left_lower_leg" -> "Left Lower Leg";
            case "eyes" -> "Eyes";
            default -> bone;
        };
    }
}

