import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Hidden Markov Model and Viterbi Algorithm application
 * @author Sajjad
 */
public class HMM
{
    HashMap<String, HashMap<String, Integer>> transitionMap = new HashMap<>(); //map of transitions curr state->next state:value
    HashMap<String, HashMap<String, Integer>> observationMap = new HashMap<>(); //map of observation curr state->word:value

    HashMap<String, Integer> totalTransition = new HashMap<>(); //the total number of transitions for each state
    HashMap<String, Integer> totalWords = new HashMap<>(); //the total number of word and their states

    HashMap<String, HashMap<String, Double>> transitionProb = new HashMap<>(); //transition log probability from one state to another
    HashMap<String, HashMap<String, Double>> wordProb = new HashMap<>(); //observation log probability

    double unseenScore = -100; //observation score if word is unseen in state

    /**
     * Training program for HMM that creates the maps for transition scores and observation scores
     * @param sentencesFile - File of sentences to train on
     * @param tagsFile - Corresponding POS to the sentences
     * @throws IOException
     */
    public void POSTraining(String sentencesFile, String tagsFile) throws IOException
    {
        BufferedReader sentenceFile = new BufferedReader(new FileReader(sentencesFile)); //sentence files
        BufferedReader tagFile = new BufferedReader(new FileReader(tagsFile)); //tag file

        String line1; //line for sentence file
        String line2; //line for tag file

        while ((line1 = sentenceFile.readLine()) != null && (line2 = tagFile.readLine()) != null) //read both files
        {
            String[] words = line1.toLowerCase().split(" "); //get the words in lower case from sentence file
            String[] tags = line2.split(" "); //gets the tags from tag file


            for (int i = 0; i < tags.length; i++) //loop through every tag in the array split to train the transitions
            {

                if (i == 0) //if it is the first tag
                {
                    if (!transitionMap.containsKey("#")) //if '#' is not in map yet, meaning first transition ever
                    {
                        HashMap<String, Integer> map = new HashMap<>(); //create a new map
                        map.put(tags[0], 1); //put the next state (first tag) and its value of 1
                        transitionMap.put("#", map); //put in the transition map the starting state and the map of next state
                        totalTransition.put("#", 1); //make total transition map value of 1 since first tag ever

                    } else { //else if '#' already in the map and it is the first tag of a different line

                        if(transitionMap.get("#").containsKey(tags[i])) //if the map already contains that transition
                        {
                            HashMap<String, Integer> map = transitionMap.get("#"); //then get the nested map from '#'
                            map.put(tags[i], map.get(tags[i]) + 1); //update the transition by 1
                            transitionMap.put("#", map); //reinsert it back in the map
                            totalTransition.put("#", totalTransition.get("#") + 1); //update the total transition map value by 1
                        }else{ //else if the map doesn't already contain that transition

                            HashMap<String, Integer> map = transitionMap.get("#"); //get the nested map from '#'
                            map.put(tags[i], 1); //put in the map the key of the next state, and make it a value of 1
                            transitionMap.put("#", map); //reinsert it back in the map
                            totalTransition.put("#", totalTransition.get("#") + 1); //update the total value by 1
                        }
                    }
                }else{ //else if it is not the first tag
                    String currTag = tags[i - 1]; //the curr tag is the previous index
                    String nextTag = tags[i]; //the next tag is the curr index

                    if (!transitionMap.containsKey(currTag)) //if the map doesn't contain the currTag as a key
                    {
                        HashMap<String, Integer> map = new HashMap<>(); //then create a new map
                        map.put(nextTag, 1); //insert the next tag in the map with value of 1
                        transitionMap.put(currTag, map); //put that map in the transition map as value and currTag as key
                        totalTransition.put(currTag, 1); //put the total transition for the current tag as 1

                    } else { //else if the map contains the currTag as a key

                        if (transitionMap.get(currTag).containsKey(nextTag)) //if currTag contains a map with key of nextTag
                        {
                            HashMap<String, Integer> map = transitionMap.get(currTag); //then get the nested map
                            map.put(nextTag, map.get(nextTag) + 1); //update the tag by 1
                            transitionMap.put(currTag, map); //reinsert the map back into transition map
                            totalTransition.put(currTag, totalTransition.get(currTag) + 1); //update the total transition score for curr tag by 1

                        } else { //else if currTag doesn't have nextTag as a key
                            HashMap<String, Integer> map = transitionMap.get(currTag); //get the nested map
                            map.put(nextTag, 1); //insert in map the next tag and value of 1
                            transitionMap.put(currTag, map); //insert in transition map with currTag as key
                            totalTransition.put(currTag, totalTransition.get(currTag) + 1); //update the total value for currTag by 1
                        }
                    }
                }
            }

            for (int i = 0; i < words.length; i++) //loop through every word to train the observations
            {
                String word = words[i]; //get the word
                String tag = tags[i]; //get its POS

                if (!observationMap.containsKey(tag)) //if the POS is not in the map
                {
                    HashMap<String, Integer> map = new HashMap<>(); //create a new map
                    map.put(word, 1); //put the word in the map with value of 1
                    observationMap.put(tag, map); //insert the map in observationMap with POS as key
                    totalWords.put(tag, 1); //make the value of the POS observation 1
                }else{ //else if POS is in the map
                    if (observationMap.get(tag).containsKey(word)) //if the POS contains the observation
                    {
                        HashMap<String, Integer> map = observationMap.get(tag); //get the nested map
                        map.put(word, map.get(word) + 1); //update its value by 1
                        observationMap.put(tag, map); //insert it back
                        totalWords.put(tag, totalWords.get(tag) + 1); //update the total value of the POS by 1
                    } else { //else if POS doesn't contain the observation
                        HashMap<String, Integer> map = observationMap.get(tag); //get the map
                        map.put(word, 1); //put the observation with value of 1
                        observationMap.put(tag, map); //reinsert the map back into observationMap
                        totalWords.put(tag, totalWords.get(tag) + 1); //update the total value of the POS by 1
                    }
                }
            }
        }

    }


    /**
     * Method to calculate the log probability of the transition scores and observation scores in the maps
     */
    public void logProbability()
    {
        //loop through the transition Map
        for (Map.Entry<String, HashMap<String, Integer>> entry : transitionMap.entrySet())
        {
            HashMap<String, Integer> map = entry.getValue(); //get the nested map
            String currState = entry.getKey(); //get the current state

            for (Map.Entry<String, Integer> m : map.entrySet()) //loop through the nested map
            {
                String nextState = m.getKey(); //get the next state as the key in nested map
                Integer value = m.getValue(); //get the score as the value of the map

                Integer totalValue = totalTransition.get(currState); //get the total value of the POS from the totalTransition map

                double val = Math.log(value) - Math.log(totalValue); //calculate the logProbability by log(val/totalVal)

                if (!transitionProb.containsKey(currState)) //if the transitionProb map doesn't contain the currState
                {
                    HashMap<String, Double> probMap = new HashMap<>(); //create a new map
                    probMap.put(nextState, val); //put the next state and its log probability for transition
                    transitionProb.put(currState, probMap); //insert the map in transitionProb map
                }else { //else if the map contains the currState
                    HashMap<String, Double> probMap = transitionProb.get(currState); ///get the nested map
                    probMap.put(nextState, val); //put the next state and its transition probability
                    transitionProb.put(currState, probMap); //reinsert the map back into transition prob

                }

            }
        }

        //loop through the observation score map
        for (Map.Entry<String, HashMap<String, Integer>> entry : observationMap.entrySet())
        {
            HashMap<String, Integer> map = entry.getValue(); //get the nested map
            String currState = entry.getKey(); //get the curr state

            for (Map.Entry<String, Integer> m : map.entrySet()) //loop through the nested map
            {
                String nextState = m.getKey(); //get the next state
                Integer value = m.getValue(); //get the score of observation

                Integer totalValue = totalWords.get(currState); //get the total score of the POS

                double val = Math.log(value) - Math.log(totalValue); //calculate the observation probability

                if (!wordProb.containsKey(currState)) //if the wordProb map doesn't contain the currState
                {
                    HashMap<String, Double> probMap = new HashMap<>(); //create a new map
                    probMap.put(nextState, val); ////put the next state and its log probability for observation
                    wordProb.put(currState, probMap); //insert the map in wordMap
                }else { //else if wordProb contains the currState
                    HashMap<String, Double> probMap = wordProb.get(currState); //get the nested map
                    probMap.put(nextState, val); //put the next state and its observation probability
                    wordProb.put(currState, probMap); //reinsert the map back into wordProb

                }
            }
        }

    }

    /**
     * Method that reads a file and calls Viterbi on the file sentences to predict its POS
     * @param file - File of test sentences
     * @return - A list of POS that corresponds to each word in the read file
     * @throws IOException
     */
    public List<String> viterbiFileReading(String file) throws IOException
    {
        BufferedReader input = new BufferedReader(new FileReader(file)); //read the file
        String line; //line in file
        List<String> paths = new ArrayList<>(); //List of POS tags

        while ((line = input.readLine()) != null) //while there is a line to read
        {
            String[] observation = line.split(" "); //parse line and get each word
            List<String> path = Viterbi(observation); //pass the String arr into Viterbi algorithm
            paths.addAll(path); //add all the POS returned from Viterbi to paths
        }
        return paths; //return the paths list of POS
    }

    /**
     * Viterbi algorithm method that takes observations and predicts its part of speech
     * @param observations - String array of words (observations)
     * @return
     */
    public List<String> Viterbi(String[] observations)
    {
        Set<String> currStates = new HashSet<>(); //Set of current states
        currStates.add("#"); //add start to current states
        Map<String, Double> currScores = new HashMap<>(); //set of current scores
        currScores.put("#", 0.0); //add start to current score, starting with score of 0

        Map<String, String> backpointers = new HashMap<>(); //map to keep track of back pointers

        for (int i = 0; i < observations.length; i++) //loop through the observations array
        {
            Set<String> nextStates = new HashSet<>(); //create a set of next states
            Map<String, Double> nextScores = new HashMap<>(); //create a map of next scores

            for (String currState : currStates) //for each state in current states
            {
                if (transitionProb.containsKey(currState)) //if transitionProb map contains that curr state
                {
                    // for each transition currState -> nextState
                    for (Map.Entry<String, Double> entry : transitionProb.get(currState).entrySet())
                    {
                        String nextState = entry.getKey(); //get the next state
                        double transitionScore = entry.getValue(); //get its transition score
                        nextStates.add(nextState); //add next state to the set

                        //get the observation score from wordProb map, or if doesn't exist, default to unseen score
                        double observationScore = wordProb.get(nextState).getOrDefault(observations[i], unseenScore);

                        //nextScore = currScores[currState] + transitionScore(currState -> nextState) + observationScore
                        double score = currScores.get(currState) + transitionScore + observationScore;

                        // if nextState isn't in nextScores or nextScore > nextScores[nextState]
                        if (!nextScores.containsKey(nextState) || score > nextScores.get(nextState))
                        {
                            nextScores.put(nextState, score); //set nextScores[nextState] to nextScore
                            backpointers.put(nextState + i, currState); // remember that pred of nextState @ i is curr
                        }

                    }
                }
            }

            currStates = nextStates; //update curr state
            currScores = nextScores; //update curr score
        }

        double maxScore = Double.NEGATIVE_INFINITY; //start with -inf as max score
        String maxState = ""; //max state

        for (Map.Entry<String, Double> entry : currScores.entrySet()) //loop through currScores map
        {
            if (entry.getValue() > maxScore) //if the score is greater than max score
            {
                maxScore = entry.getValue(); //update the max score
                maxState = entry.getKey(); //update the max state
            }
        }

        List<String> path = new ArrayList<>(); //path of the POS
        path.add(maxState); //add max state to the path

        for (int i = observations.length - 1; i >= 1; i--) //loop backwards through the observation arr
        {
            String backpointer = backpointers.get(path.get(0) + i); //get the back pointer
            path.add(0, backpointer); //add to the path the back pointer
        }

        return path; //return the path
    }

    /**
     * Method to compare performance from Viterbi predictions to actual POS tags
     * @param tagFile - original tag file to compare performance
     * @param wordFile - word file to do viterbi algorithm on and compare results
     * @throws IOException
     */
    public void comparePerformance(String tagFile, String wordFile) throws IOException
    {
        BufferedReader input = new BufferedReader(new FileReader(tagFile)); //read the tag file
        String line; //line in file
        List<String> originalTags = new ArrayList<>(); //original correct POS tags

        while ((line = input.readLine()) != null) //while there is a line to read
        {
            String[] tags = line.split(" "); //get the String arr of observations
            originalTags.addAll(List.of(tags)); //add all the tags to the tag list
        }

        List<String> viterbiTags = viterbiFileReading(wordFile); //list of viterbiTags returned by viterbi after passing sentence file

        int total = originalTags.size(); //the total number of POS tags
        int matching = 0; //variable to keep track of matching number of tags from viterbi prediction to original

        for (int i = 0; i < total; i++) //loop through total number of POS tags
        {
            if (originalTags.get(i).equals(viterbiTags.get(i))) //if the POS tag from viterbi matches that of the original
            {
                matching++; //update the matching score by 1
            }
        }
        System.out.println("Number of tags correct: " + matching); //print number of correct tags
        System.out.println("Number of tags incorrect: " + (total-matching)); //number of incorrect tags (total-matching)
        System.out.println("Total number of tags: " + total); //total number of tags
    }

    /**
     * Console testing method that takes user input and predicts the POS
     */
    public void consoleTesting()
    {
        Scanner userInput = new Scanner(System.in); //instantiate user input
        System.out.println("Please time a sentence in to check: "); //prompt user for a sentence

        String line = userInput.nextLine(); //retrieve sentence user input
        String[] words = line.toLowerCase().split(" "); //split the sentence by space and make it lower case
        List<String> tags = Viterbi(words); //get the list of tags by calling viterbi on the String array

        for (int i = 0; i < words.length; i++) //loop through the words
        {
            words[i] += "/" + tags.get(i); //update the words by marking it with the POS corresponding to that index from viterbi
        }
        for (String word : words) //for each word
        {
            System.out.print(word + " "); //print out the word with its POS tag
        }
    }

    public static void main(String[] args) throws IOException {
        HMM hmm = new HMM(); //instantiate object of HMM

        //POS training by passing in sentences and tags
        hmm.POSTraining("inputs/brown-train-sentences.txt","inputs/brown-train-tags.txt");
        hmm.logProbability(); //calculate the log probability of transitions and observations on the maps

        //compare the performance (file based testing) by passing in the correct tags and the file to be predicted through Viterbi
        hmm.comparePerformance("inputs/brown-test-tags.txt","inputs/brown-test-sentences.txt");
        hmm.consoleTesting(); //console based testing
    }
}
