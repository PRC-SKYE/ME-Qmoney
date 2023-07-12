
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;

import com.crio.warmup.stock.portfolio.PortfolioManagerImpl;



public class PortfolioManagerApplication {




  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  public static List < String > mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    File inputFile = resolveFileFromResources(args[0]);
    String endDate = args[1];
    ObjectMapper objectMapper = getObjectMapper();
    ArrayList < TotalReturnsDto > symbolClosingValueList = new ArrayList < TotalReturnsDto > ();
    PortfolioTrade[] trades = objectMapper.readValue(inputFile, PortfolioTrade[].class);
    for (PortfolioTrade trade: trades) {
      List<Candle> candleList = fetchCandles(trade, LocalDate.parse(endDate), getToken());
      TotalReturnsDto returnDto = new TotalReturnsDto(trade.getSymbol(), getClosingPriceOnEndDate(candleList));
      symbolClosingValueList.add(returnDto);
    }
    
    Comparator<TotalReturnsDto> comp = (i,j)->i.getClosingPrice()>j.getClosingPrice()?1:-1;
    Collections.sort(symbolClosingValueList,comp);
    List < String > symbolList = new ArrayList < String > ();
    for (TotalReturnsDto symbol: symbolClosingValueList) {
      symbolList.add(symbol.getSymbol());
    }
    return symbolList;
  }

  public static List < PortfolioTrade > readTradesFromJson(String filename) throws IOException, URISyntaxException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    String contents = readFileAsString(filename);
    PortfolioTrade[] trades = objectMapper.readValue(contents, PortfolioTrade[].class);
    return Arrays.asList(trades);
  }


  public static List < String > debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/lochanbisne04-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@1573f9fc";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";

    return Arrays.asList(new String[] {
      valueOfArgument0,
      resultOfResolveFilePathArgs0,
      toStringOfObjectMapper,
      functionNameFromTestFileInStackTrace,
      lineNumberFromTestFileInStackTrace
    });
  }
  
  public static String getToken() {
    return "ef8701e3877cbb0d553d90b9cc905d4462a2a15f";
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
      Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Candle openingCandle = candles.get(0);
    return  openingCandle.getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Candle closingCandle = candles.get(candles.size() - 1);
    return  closingCandle.getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    RestTemplate restTemplate = new RestTemplate();
    String symbolResourceUrl = prepareUrl(trade, endDate, token);
    Candle[] candles = restTemplate.getForObject(symbolResourceUrl, TiingoCandle[].class);
    List<Candle> candleList = Arrays.asList(candles);
    return candleList;
  }


  public static List < String > mainReadFile(String[] args) throws IOException, URISyntaxException {
    File inputFile = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(inputFile,PortfolioTrade[].class);
    List<String> arr = new ArrayList<>();
   
    for(int i=0;i<trades.length;i++){
      System.out.println(trades[i].getSymbol());
      arr.add(trades[i].getSymbol());
    }
    return arr;
  }
  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String url = "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() +
      "/prices?startDate=" + trade.getPurchaseDate() +
      "&endDate=" + endDate + "&token=" + token;
    return url;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
        File inputFile = resolveFileFromResources(args[0]);
        LocalDate endDate = LocalDate.parse(args[1]);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(inputFile, PortfolioTrade[].class);
        PortfolioManager portfolioManager = new PortfolioManagerImpl(new RestTemplate());
        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        double totalYears =  ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate)/365.0;
        double totalReturns = (sellPrice - buyPrice) / buyPrice;
        double annualizedReturn = Math.pow((1+totalReturns), (1/totalYears)) - 1;
        System.out.println(annualizedReturn);
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns);
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }
  private static String readFileAsString(String file) throws URISyntaxException, IOException{
    return new String(Files.readAllBytes(resolveFileFromResources(file).toPath()),"UTF-8");
  }




  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
        String file = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        String contents = readFileAsString(file);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
        PortfolioManager portfolioManager = new PortfolioManagerImpl(new RestTemplate());
        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());




    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}

