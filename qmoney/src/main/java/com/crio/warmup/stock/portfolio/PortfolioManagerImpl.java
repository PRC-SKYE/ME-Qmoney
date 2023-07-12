
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  public PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
    LocalDate endDate) {
      List <AnnualizedReturn> annualizedReturnList = new ArrayList <AnnualizedReturn> ();
      for (PortfolioTrade trade: portfolioTrades) {
        List<Candle> candleList;
        try {
          candleList = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
          double sellPrice = getClosingPriceOnEndDate(candleList);
          double buyPrice = getOpeningPriceOnStartDate(candleList);
          double totalYears =  ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate)/365.0;
          double totalReturns = (sellPrice - buyPrice) / buyPrice;
          double annualizedReturn = Math.pow((1+totalReturns), (1/totalYears)) - 1;
          annualizedReturnList.add(new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns));
          annualizedReturnList.sort(getComparator());
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      }
      return annualizedReturnList;
  }

  static Double getOpeningPriceOnStartDate(List < Candle > candles) {
    Candle openingCandle = candles.get(0);
    return  openingCandle.getOpen();
  }

  public static Double getClosingPriceOnEndDate(List < Candle > candles) {
    Candle closingCandle = candles.get(candles.size() - 1);
    return  closingCandle.getClose();
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
        String symbolResourceUrl = buildUri(symbol, from, to);
        Candle[] candles = restTemplate.getForObject(symbolResourceUrl, TiingoCandle[].class);
        return  Arrays.asList(candles);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
      String token = "8776f3e49963d36fa7808d55e489abc60bbf3562";
      String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
      String uri = uriTemplate.replace("$SYMBOL", symbol)
        .replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString())
        .replace("$APIKEY", token);
      return uri;
  }
}

