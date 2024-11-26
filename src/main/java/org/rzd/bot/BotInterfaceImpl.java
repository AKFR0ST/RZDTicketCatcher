package org.rzd.bot;

import org.json.JSONObject;
import org.rzd.model.ApplicationOptions;
import org.rzd.model.TicketOptions;
import org.rzd.server.CatchersServerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component ("BotApiImpl")
public class BotInterfaceImpl implements BotInterface {

    ApplicationContext applicationContext;
    CatchersServerImpl server;
    ApplicationOptions options;
    public Long offset;

    public BotInterfaceImpl() {

    }

    @Autowired
    public BotInterfaceImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        server = applicationContext.getBean(CatchersServerImpl.class);
        options = applicationContext.getBean(ApplicationOptions.class);
        offset = 0L;
    }

    private String readOneMessage() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "https://api.telegram.org/bot%s:%s/getUpdates?limit=1&offset=%d";
        String url = String.format(baseUrl, options.getBotId(), options.getApiKey(), offset);
        String response;
        JSONObject jsonObject;

        do {
            response = restTemplate.getForEntity(url, String.class).getBody();
            if(response==null){
                throw new RuntimeException("Response is null");
            }
            jsonObject = new JSONObject(response);
        } while (jsonObject.getJSONArray("result").isEmpty());

        offset = jsonObject.getJSONArray("result").getJSONObject(0).getLong("update_id") + 1;
        return response;
    }

    public void start() {
        String message = "";
        do {
            String response = readOneMessage();
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("ok") && !jsonObject.getJSONArray("result").isEmpty()) {

                Long chatId = jsonObject.
                        getJSONArray("result").
                        getJSONObject(0).
                        getJSONObject("message").
                        getJSONObject("chat").
                        getLong("id");

                message = jsonObject.
                        getJSONArray("result").
                        getJSONObject(0).
                        getJSONObject("message").
                        getString("text");

                switch (message) {
                    case ("/all"):
                        allCatchers(chatId);
                        break;
                    case ("/kill"):
                        killCatcher(chatId);
                        break;
                    case ("/active"):
                        activeCatchers(chatId);
                        break;
                    case ("/new"):
                        newCatcher(chatId);
                        break;
                    case ("/stop"):
                        stopServer();
                        break;
                }
            }
        } while (!message.equals("/stop")) ;
    }

    @Override
    public void allCatchers(Long chatId) {
        sendMessage(chatId, server.allCatchers());
    }

    @Override
    public void activeCatchers(Long chatId) {
        sendMessage(chatId, server.activeCatchers());
    }

    @Override
    public void killCatcher(Long chatId) {
        sendMessage(chatId, "Выберете кэтчер для остановки:\n"+server.activeCatchers());
        String response = readOneMessage();
        JSONObject jsonObject = new JSONObject(response);
        String idCatcherToKill = jsonObject.getJSONArray("result").getJSONObject(0).getJSONObject("message").getString("text");
        int result = server.killCatcherById(Long.parseLong(idCatcherToKill));
        String resultMessage = "";
        if(result==0){
            resultMessage = "Catcher with id " + idCatcherToKill + "killed";
        }
        if(result==1){
            resultMessage = "Catcher with id " + idCatcherToKill + "not killed";
        }
        if(result==2){
            resultMessage = "Catcher with id " + idCatcherToKill + "not found";
        }
        sendMessage(chatId, resultMessage);
    }

    @Override
    public void newCatcher(Long chatId) {
        TicketOptions ticketOptions = new TicketOptions();
        sendMessage(chatId, "Введите код станции отправления");
        ticketOptions.setCode0(getTextMessage(readOneMessage()));
        sendMessage(chatId, "Введите код станции назначения");
        ticketOptions.setCode1(getTextMessage(readOneMessage()));
        sendMessage(chatId, "Введите дату отправления");
        ticketOptions.setDt0(getTextMessage(readOneMessage()));
        sendMessage(chatId, "Введите номер поезда");
        ticketOptions.setNumber(getTextMessage(readOneMessage()));
        sendMessage(chatId, "Введите тип вагона");
        ticketOptions.setType(getTextMessage(readOneMessage()));
        sendMessage(chatId, "Введите максимальную цену билета");
        ticketOptions.setMaxPrice(Long.parseLong(getTextMessage(readOneMessage())));
        server.newCatcher(ticketOptions, chatId);

    }

    private void stopServer(){
        server.stop();
    }

    private String getTextMessage(String response) {
        JSONObject jsonObject = new JSONObject(response);
        return jsonObject.getJSONArray("result").getJSONObject(0).getJSONObject("message").getString("text");
    }

    public void sendMessage(Long chatId, String message) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.telegram.org/bot"+
                options.getBotId()+
                ":"+
                options.getApiKey()+
                "/sendMessage?chat_id="+
                chatId+
                "&text="+
                message;
        restTemplate.getForEntity(url, String.class);
    }
}