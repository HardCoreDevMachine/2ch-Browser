package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

import ua.in.quireg.chan.common.utils.StringUtils;

public class ApiError {
    @JsonProperty("code")
    public int code;
    @JsonProperty("message")
    public String message;

    /**
     * Human-readable error text: the server message when it is non-blank
     * (e.g. ban reason), otherwise a description of the code from the
     * 2ch API (api.yml ErrorCode schema).
     */
    public String getMessage() {
        if (!StringUtils.isEmptyOrWhiteSpace(message)) {
            return message;
        }
        switch (code) {
            case 0: return "Ошибки нет.";
            case 403: return "Ошибка доступа.";
            case 666: return "Внутренняя ошибка сервера.";
            case 667: return "Запрос не существует.";
            case -2: return "Доска не существует.";
            case -3: return "Тред не существует.";
            case -31: return "Пост не существует.";
            case -4: return "Нет доступа к запрошенному контенту.";
            case -41: return "Доска закрыта.";
            case -42: return "Доступ к доске возможен только с пасскодом.";
            case -5: return "Капча не валидна.";
            case -6: return "Вы были забанены.";
            case -7: return "Тред закрыт.";
            case -8: return "Вы постите слишком быстро или достигнут лимит создания тредов.";
            case -9: return "Поле слишком большое.";
            case -10: return "Похожий файл уже был загружен.";
            case -11: return "Файл не поддерживается.";
            case -12: return "Слишком большой файл.";
            case -13: return "Загружено больше файлов, чем разрешено на доске.";
            case -14: return "Трипкод был забанен.";
            case -15: return "В комментарии недопустимое выражение.";
            case -16: return "В комментарии выражение из спамлиста.";
            case -19: return "При создании треда необходимо загрузить файл.";
            case -20: return "Пост не может быть пустым.";
            case -21: return "Пасскод не существует.";
            case -22: return "Достигнут лимит запросов, попробуйте позже.";
            case -23: return "Слишком короткое сообщение.";
            case -50: return "Слишком много постов для жалобы.";
            case -51: return "Вы ничего не написали в жалобе.";
            case -52: return "Вы уже отправляли жалобу.";
            case -300: return "Приложение не существует или было отключено.";
            case -301: return "Некорректный идентификатор приложения.";
            case -302: return "Идентификатор приложения истёк.";
            case -303: return "Неверная подпись поста с помощью идентификатора.";
            case -304: return "Указанный идентификатор уже был использован.";
            case -24: return "Некорректный идентификатор стикера.";
            case -25: return "Стикер не найден.";
            default: return "Неизвестная ошибка (код " + code + ").";
        }
    }
}
