package dk.kb.dod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import dk.kb.alma.gen.Bib;
import dk.kb.alma.gen.Item;
import dk.kb.alma.gen.Request;
import dk.kb.alma.gen.Requests;
import dk.kb.alma.gen.User;
import dk.kb.alma.gen.WebServiceResult;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class AlmaClient {

    protected final static Logger log = LoggerFactory.getLogger(AlmaClient.class);


    public static final String APIKEY = "apikey";

    private final String alma_apikey;


    //Select the right one based on which part of the world you are in
    private final String almaTarget;

    private final Cache<URI, Object> cache;
    private final Striped<Lock> locks;


    private final String lang;
    private final long minSleepMillis;
    private final long sleepVariationMillis;

    public AlmaClient(String almaTarget, String alma_apikey, long minSleep, long sleepVariation, String lang) {
        this.almaTarget = almaTarget;
        this.alma_apikey = alma_apikey;
        this.minSleepMillis = minSleep;
        this.sleepVariationMillis = sleepVariation;
        this.lang = lang;

        int cacheSize = 1000;
        cache = CacheBuilder.newBuilder()
                            .maximumSize(cacheSize)
                            .expireAfterAccess(5, TimeUnit.HOURS)
                            .build();

        locks = Striped.lock(cacheSize);

    }
    public WebClient constructLink(){
        return getWebClient(almaTarget);
    }

    public WebClient getWebClient(String link) {
        WebClient webClient = WebClient.create(link).accept(MediaType.APPLICATION_XML_TYPE);
        if (lang != null) {
            webClient = webClient.query("lang", lang);
        }
        return webClient;
    }
    public <T> T getLinkValue(final String link, Class<T> type)  {
        return getLinkValue(getWebClient(link),type);
    }

    public <T> T getLinkValue(final WebClient link, Class<T> type)  {
        return getLinkValue(link,type,true);
    }

    protected  <T> T getLinkValue(final WebClient link, Class<T> type, boolean useCache) {
        URI currentURI = link.getCurrentURI();
        Lock lock = locks.get(currentURI);
        lock.lock();
        try {
            if (useCache) {
                Object cacheValue = cache.getIfPresent(currentURI);
                if (type.isInstance(cacheValue)) {
                    log.trace("cache hit on {}", currentURI);
                    return (T) cacheValue;
                }
            }
            log.trace("Fetching {}", currentURI);
            T value;
            try {
                value = link.replaceQueryParam(APIKEY, alma_apikey).get(type);
                log.trace("Fetched {}", currentURI);
            } catch (WebApplicationException e) {
                if (rateLimitSleep(e)) {
                    return getLinkValue(link, type);
                }

                Response response = e.getResponse();
                log.warn("Failed to retrieve '{}', trying to parse out webservice result from ALMA reply",
                         currentURI, e);
                WebServiceResult result = response.readEntity(WebServiceResult.class);
                if (result.isErrorsExist()) {
                    String errorMessage = result.getErrorList()
                                                .getErrors()
                                                .stream()
                                                .map(error -> error.getErrorMessage())
                                                .collect(
                                                        Collectors.joining(", "));
                    String errorCode = result.getErrorList()
                                             .getErrors()
                                             .stream()
                                             .findFirst()
                                             .map(error -> error.getErrorCode())
                                             .orElseGet(null);
                    //TODO make this clever like in labels
                    throw new RuntimeException("Failed to retrieve '" + currentURI + "'", e);

                } else {
                    throw new RuntimeException("Failed to retrieve '" + currentURI + "'", e);
                }
            }
            if (useCache) {
                cache.put(currentURI, value);
            }
            return value;

        } finally {
            link.close();
            lock.unlock();
        }
    }



    /**
     * If the exception is a rate-limit, then sleep for the defined time and return true.
     * Otherwise return false immediately
     * <p>
     * The duration of sleep will be
     * <p>
     * long sleepTimeMillis = minSleepMillis + Math.round(Math.random() * sleepVariationMillis);
     *
     * @param e the web application exception
     * @return true if the error was a rate limit
     * @see #minSleepMillis
     * @see #sleepVariationMillis
     */
    private boolean rateLimitSleep(WebApplicationException e) {
        if (429 == e.getResponse().getStatusInfo().getStatusCode()) {
            long sleepTimeMillis = minSleepMillis + Math.round(Math.random() * sleepVariationMillis);
            log.warn("Received response status 429, rate-limiting, so backing off for {} seconds",
                     Math.round(sleepTimeMillis / 1000.0));
            try {
                Thread.sleep(sleepTimeMillis);
            } catch (InterruptedException ex) {
            }
            return true;
        }
        return false;
    }


    public Item getItem(String barcode){
        return getLinkValue(constructLink().path("/items/").query("item_barcode", barcode), Item.class);
    }


    public Bib getBib(String mmsID) {
        return getLinkValue(constructLink().path("/bibs/").path(mmsID), Bib.class);
    }


    public User getUser(String userID) {
        return getLinkValue(constructLink().path("/users/").path(userID), User.class);
    }

    public Requests getRequests(String mmsID) {
        return getLinkValue(constructLink().path("/bibs/").path(mmsID).path("/requests"),Requests.class);
    }
}
