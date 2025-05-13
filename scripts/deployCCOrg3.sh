#!/bin/bash
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/configtx
export VERBOSE=false

source scripts/utils.sh

function printHelpOrg3() {
    println "Usage: "
    println "  ./scripts/deployCCOrg3.sh [Flags]"
    println
    println "    Flags:"
    println "    -c <channel name> - Name of channel to deploy chaincode to"
    println "    -ccn <name> - Chaincode name."
    println "    -ccl <language> - Programming language of chaincode to deploy: go, java, javascript, typescript"
    println "    -ccv <version>  - Chaincode version. 1.0 (default), v2, version3.x, etc"
    println "    -ccs <sequence>  - Chaincode definition sequence. Must be an integer, 1 (default), 2, 3, etc"
    println "    -ccp <path>  - File path to the chaincode."
    println "    -ccep <policy>  - (Optional) Chaincode endorsement policy using signature policy syntax. The default policy requires an endorsement from Org1 and Org2"
    println "    -cccg <collection-config>  - (Optional) File path to private data collections configuration file"
    println "    -cci <fcn name>  - (Optional) Name of chaincode initialization function. When a function is provided, the execution of init will be requested and the function will be invoked."
    println
    println "    -h - Print this message"
    println
    println " Possible Mode and flag combinations"
    println "   -ccn -ccl -ccv -ccs -ccp -cci -r -d -verbose"
    println
    println " Examples:"
    println "   ./scripts/deployCCOrg3.sh -ccn basic -ccp ../asset-transfer-basic/chaincode-javascript/ ./ -ccl javascript"
    println "   ./scripts/deployCCOrg3.sh -ccn mychaincode -ccp ./user/mychaincode -ccv 1 -ccl javascript"
}

function deployCC() {
    
    CHANNEL_NAME=${1:-"mychannel"}
    CC_NAME=${2}
    CC_SRC_PATH=${3}
    CC_SRC_LANGUAGE=${4}
    CC_VERSION=${5:-"1.0"}
    CC_SEQUENCE=${6:-"2"}
    CC_INIT_FCN=${7:-"NA"}
    CC_END_POLICY=${8:-"NA"}
    CC_COLL_CONFIG=${9:-"NA"}
    DELAY=${10:-"3"}
    MAX_RETRY=${11:-"5"}
    VERBOSE=${12:-"false"}

    println "executing with the following"
    println "- CHANNEL_NAME: ${C_GREEN}${CHANNEL_NAME}${C_RESET}"
    println "- CC_NAME: ${C_GREEN}${CC_NAME}${C_RESET}"
    println "- CC_SRC_PATH: ${C_GREEN}${CC_SRC_PATH}${C_RESET}"
    println "- CC_SRC_LANGUAGE: ${C_GREEN}${CC_SRC_LANGUAGE}${C_RESET}"
    println "- CC_VERSION: ${C_GREEN}${CC_VERSION}${C_RESET}"
    println "- CC_SEQUENCE: ${C_GREEN}${CC_SEQUENCE}${C_RESET}"
    println "- CC_END_POLICY: ${C_GREEN}${CC_END_POLICY}${C_RESET}"
    println "- CC_COLL_CONFIG: ${C_GREEN}${CC_COLL_CONFIG}${C_RESET}"
    println "- CC_INIT_FCN: ${C_GREEN}${CC_INIT_FCN}${C_RESET}"
    println "- DELAY: ${C_GREEN}${DELAY}${C_RESET}"
    println "- MAX_RETRY: ${C_GREEN}${MAX_RETRY}${C_RESET}"
    println "- VERBOSE: ${C_GREEN}${VERBOSE}${C_RESET}"

    FABRIC_CFG_PATH=$PWD/../config/

    #User has not provided a name
    if [ -z "$CC_NAME" ] || [ "$CC_NAME" = "NA" ]; then
    fatalln "No chaincode name was provided. Valid call example: ./scripts/deployCCOrg3.sh -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

    # User has not provided a path
    elif [ -z "$CC_SRC_PATH" ] || [ "$CC_SRC_PATH" = "NA" ]; then
    fatalln "No chaincode path was provided. Valid call example: ./scripts/deployCCOrg3.sh -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

    # User has not provided a language
    elif [ -z "$CC_SRC_LANGUAGE" ] || [ "$CC_SRC_LANGUAGE" = "NA" ]; then
    fatalln "No chaincode language was provided. Valid call example: ./scripts/deployCCOrg3.sh -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

    ## Make sure that the path to the chaincode exists
    elif [ ! -d "$CC_SRC_PATH" ]; then
    fatalln "Path to chaincode does not exist. Please provide different path."
    fi

    CC_SRC_LANGUAGE=$(echo "$CC_SRC_LANGUAGE" | tr [:upper:] [:lower:])

    # do some language specific preparation to the chaincode before packaging
    if [ "$CC_SRC_LANGUAGE" = "go" ]; then
    CC_RUNTIME_LANGUAGE=golang

    infoln "Vendoring Go dependencies at $CC_SRC_PATH"
    pushd $CC_SRC_PATH
    GO111MODULE=on go mod vendor
    popd
    successln "Finished vendoring Go dependencies"

    elif [ "$CC_SRC_LANGUAGE" = "java" ]; then
    CC_RUNTIME_LANGUAGE=java

    infoln "Compiling Java code..."
    pushd $CC_SRC_PATH
    ./gradlew installDist
    popd
    successln "Finished compiling Java code"
    CC_SRC_PATH=$CC_SRC_PATH/build/install/$CC_NAME

    elif [ "$CC_SRC_LANGUAGE" = "javascript" ]; then
    CC_RUNTIME_LANGUAGE=node

    elif [ "$CC_SRC_LANGUAGE" = "typescript" ]; then
    CC_RUNTIME_LANGUAGE=node

    infoln "Compiling TypeScript code into JavaScript..."
    pushd $CC_SRC_PATH
    npm install
    npm run build
    popd
    successln "Finished compiling TypeScript code into JavaScript"

    else
    fatalln "The chaincode language ${CC_SRC_LANGUAGE} is not supported by this script. Supported chaincode languages are: go, java, javascript, and typescript"
    exit 1
    fi

    INIT_REQUIRED="--init-required"
    # check if the init fcn should be called
    if [ "$CC_INIT_FCN" = "NA" ]; then
    INIT_REQUIRED=""
    fi

    if [ "$CC_END_POLICY" = "NA" ]; then
    CC_END_POLICY=""
    else
    CC_END_POLICY="--signature-policy $CC_END_POLICY"
    fi

    if [ "$CC_COLL_CONFIG" = "NA" ]; then
    CC_COLL_CONFIG=""
    else
    CC_COLL_CONFIG="--collections-config $CC_COLL_CONFIG"
    fi

    # import utils
    . scripts/envVar.sh

    packageChaincode() {
    set -x
    peer lifecycle chaincode package ${CC_NAME}.tar.gz --path ${CC_SRC_PATH} --lang ${CC_RUNTIME_LANGUAGE} --label ${CC_NAME}_${CC_VERSION} >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    verifyResult $res "Chaincode packaging has failed"
    successln "Chaincode is packaged"
    }

    # installChaincode PEER ORG
    installChaincode() {
    ORG=$1
    setGlobals $ORG
    set -x
    peer lifecycle chaincode install ${CC_NAME}.tar.gz >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    verifyResult $res "Chaincode installation on peer0.org${ORG} has failed"
    successln "Chaincode is installed on peer0.org${ORG}"
    }

    # queryInstalled PEER ORG
    queryInstalled() {
    ORG=$1
    setGlobals $ORG
    set -x
    peer lifecycle chaincode queryinstalled >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    PACKAGE_ID=$(sed -n "/${CC_NAME}_${CC_VERSION}/{s/^Package ID: //; s/, Label:.*$//; p;}" log.txt)
    verifyResult $res "Query installed on peer0.org${ORG} has failed"
    successln "Query installed successful on peer0.org${ORG} on channel"
    }

    # approveForMyOrg VERSION PEER ORG
    approveForMyOrg() {
    ORG=$1
    setGlobals $ORG
    set -x
    peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --package-id ${PACKAGE_ID} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    verifyResult $res "Chaincode definition approved on peer0.org${ORG} on channel '$CHANNEL_NAME' failed"
    successln "Chaincode definition approved on peer0.org${ORG} on channel '$CHANNEL_NAME'"
    }

    # checkCommitReadiness VERSION PEER ORG
    checkCommitReadiness() {
    ORG=$1
    shift 1
    setGlobals $ORG
    infoln "Checking the commit readiness of the chaincode definition on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
    local rc=1
    local COUNTER=1
    # continue to poll
    # we either get a successful response, or reach MAX RETRY
    while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
        sleep $DELAY
        infoln "Attempting to check the commit readiness of the chaincode definition on peer0.org${ORG}, Retry after $DELAY seconds."
        set -x
        peer lifecycle chaincode checkcommitreadiness --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} --output json >&log.txt
        res=$?
        { set +x; } 2>/dev/null
        let rc=0
        for var in "$@"; do
        grep "$var" log.txt &>/dev/null || let rc=1
        done
        COUNTER=$(expr $COUNTER + 1)
    done
    cat log.txt
    if test $rc -eq 0; then
        infoln "Checking the commit readiness of the chaincode definition successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
    else
        fatalln "After $MAX_RETRY attempts, Check commit readiness result on peer0.org${ORG} is INVALID!"
    fi
    }

    # commitChaincodeDefinition VERSION PEER ORG (PEER ORG)...
    commitChaincodeDefinition() {
    parsePeerConnectionParameters $@
    res=$?
    verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

    # while 'peer chaincode' command can get the orderer endpoint from the
    # peer (if join was successful), let's supply it directly as we know
    # it using the "-o" option
    set -x
    peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA --channelID $CHANNEL_NAME --name ${CC_NAME} $PEER_CONN_PARMS --version ${CC_VERSION} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    verifyResult $res "Chaincode definition commit failed on peer0.org${ORG} on channel '$CHANNEL_NAME' failed"
    successln "Chaincode definition committed on channel '$CHANNEL_NAME'"
    }

    # queryCommitted ORG
    queryCommitted() {
    ORG=$1
    setGlobals $ORG
    EXPECTED_RESULT="Version: ${CC_VERSION}, Sequence: ${CC_SEQUENCE}, Endorsement Plugin: escc, Validation Plugin: vscc"
    infoln "Querying chaincode definition on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
    local rc=1
    local COUNTER=1
    # continue to poll
    # we either get a successful response, or reach MAX RETRY
    while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
        sleep $DELAY
        infoln "Attempting to Query committed status on peer0.org${ORG}, Retry after $DELAY seconds."
        set -x
        peer lifecycle chaincode querycommitted --channelID $CHANNEL_NAME --name ${CC_NAME} >&log.txt
        res=$?
        { set +x; } 2>/dev/null
        test $res -eq 0 && VALUE=$(cat log.txt | grep -o '^Version: '$CC_VERSION', Sequence: [0-9]*, Endorsement Plugin: escc, Validation Plugin: vscc')
        test "$VALUE" = "$EXPECTED_RESULT" && let rc=0
        COUNTER=$(expr $COUNTER + 1)
    done
    cat log.txt
    if test $rc -eq 0; then
        successln "Query chaincode definition successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
    else
        fatalln "After $MAX_RETRY attempts, Query chaincode definition result on peer0.org${ORG} is INVALID!"
    fi
    }

    chaincodeInvokeInit() {
    parsePeerConnectionParameters $@
    res=$?
    verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

    # while 'peer chaincode' command can get the orderer endpoint from the
    # peer (if join was successful), let's supply it directly as we know
    # it using the "-o" option
    set -x
    fcn_call='{"function":"'${CC_INIT_FCN}'","Args":[]}'
    infoln "invoke fcn call:${fcn_call}"
    peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n ${CC_NAME} $PEER_CONN_PARMS --isInit -c ${fcn_call} >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    cat log.txt
    verifyResult $res "Invoke execution on $PEERS failed "
    successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
    }

    chaincodeQuery() {
    ORG=$1
    setGlobals $ORG
    infoln "Querying on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
    local rc=1
    local COUNTER=1
    # continue to poll
    # we either get a successful response, or reach MAX RETRY
    while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
        sleep $DELAY
        infoln "Attempting to Query peer0.org${ORG}, Retry after $DELAY seconds."
        set -x
        peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"Args":["queryAllCars"]}' >&log.txt
        res=$?
        { set +x; } 2>/dev/null
        let rc=$res
        COUNTER=$(expr $COUNTER + 1)
    done
    cat log.txt
    if test $rc -eq 0; then
        successln "Query successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
    else
        fatalln "After $MAX_RETRY attempts, Query result on peer0.org${ORG} is INVALID!"
    fi
    }

    ## package the chaincode
    packageChaincode

    ## Install chaincode on peers
    infoln "Installing chaincode on peer0.org1..."
    installChaincode 1
    infoln "Install chaincode on peer0.org2..."
    installChaincode 2
    infoln "Installing chaincode on peer0.org3..."
    installChaincode 3

    ## query whether the chaincode is installed
    queryInstalled 3

    ## approve the definition for org1
    approveForMyOrg 1

    ## check whether the chaincode definition is ready to be committed
    ## expect org1 to have approved and org2 not to
    checkCommitReadiness 1 "\"Org1MSP\": true" "\"Org2MSP\": false" "\"Org3MSP\": false"
    checkCommitReadiness 2 "\"Org1MSP\": true" "\"Org2MSP\": false" "\"Org3MSP\": false"
    checkCommitReadiness 3 "\"Org1MSP\": true" "\"Org2MSP\": false" "\"Org3MSP\": false"

    ## now approve also for org2
    approveForMyOrg 2

    ## check whether the chaincode definition is ready to be committed
    ## expect them both to have approved
    checkCommitReadiness 1 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": false"
    checkCommitReadiness 2 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": false"
    checkCommitReadiness 3 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": false"

    ## approve the definition for org1
    approveForMyOrg 3

    ## check whether the chaincode definition is ready to be committed
    ## expect them both to have approved
    checkCommitReadiness 1 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": true"
    checkCommitReadiness 2 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": true"
    checkCommitReadiness 3 "\"Org1MSP\": true" "\"Org2MSP\": true" "\"Org3MSP\": true"

    ## now that we know for sure orgs have approved, commit the definition
    commitChaincodeDefinition 1 2 3

    ## query on both orgs to see that the definition committed successfully
    queryCommitted 1
    queryCommitted 2
    queryCommitted 3

    ## Invoke the chaincode - this does require that the chaincode have the 'initLedger'
    ## method defined
    if [ "$CC_INIT_FCN" = "NA" ]; then
    infoln "Chaincode initialization is not required"
    else
    chaincodeInvokeInit 1 2 3
    fi

    exit 0
 if [ $? -ne 0 ]; then
    fatalln "Deploying chaincode failed"
  fi
}


# Obtain the OS and Architecture string that will be used to select the correct
# native binaries for your platform, e.g., darwin-amd64 or linux-amd64
OS_ARCH=$(echo "$(uname -s | tr '[:upper:]' '[:lower:]' | sed 's/mingw64_nt.*/windows/')-$(uname -m | sed 's/x86_64/amd64/g')" | awk '{print tolower($0)}')
# Using crpto vs CA. default is cryptogen
CRYPTO="cryptogen"
# timeout duration - the duration the CLI should wait for a response from
# another container before giving up
MAX_RETRY=5
# default for delay between commands
CLI_DELAY=3
# channel name defaults to "mychannel"
CHANNEL_NAME="mychannel"
# chaincode name defaults to "NA"
CC_NAME="NA"
# chaincode path defaults to "NA"
CC_SRC_PATH="NA"
# endorsement policy defaults to "NA". This would allow chaincodes to use the majority default policy.
CC_END_POLICY="NA"
# collection configuration defaults to "NA"
CC_COLL_CONFIG="NA"
# chaincode init function defaults to "NA"
CC_INIT_FCN="NA"
# use this as the default docker-compose yaml definition
COMPOSE_FILE_BASE=docker/docker-compose-test-net.yaml
# docker-compose.yaml file if you are using couchdb
COMPOSE_FILE_COUCH=docker/docker-compose-couch.yaml
# certificate authorities compose file
COMPOSE_FILE_CA=docker/docker-compose-ca.yaml
# use this as the docker compose couch file for org3
COMPOSE_FILE_COUCH_ORG3=addOrg3/docker/docker-compose-couch-org3.yaml
# use this as the default docker-compose yaml definition for org3
COMPOSE_FILE_ORG3=addOrg3/docker/docker-compose-org3.yaml
#
# chaincode language defaults to "NA"
CC_SRC_LANGUAGE="NA"
# Chaincode version
CC_VERSION="1.0"
# Chaincode definition sequence
CC_SEQUENCE=1
# default image tag
IMAGETAG="latest"
# default ca image tag
CA_IMAGETAG="latest"
# default database
DATABASE="leveldb"

# Parse commandline args

## Parse mode
if [[ $# -lt 1 ]] ; then
  printHelpOrg3
  exit 0
else
  MODE="deployCC"
fi


# parse flags

while [[ $# -ge 1 ]] ; do
  key="$1"
  case $key in
  -h )
    printHelpOrg3 $MODE
    exit 0
    ;;
  -c )
    CHANNEL_NAME="$2"
    shift
    ;;
  -ca )
    CRYPTO="Certificate Authorities"
    ;;
  -r )
    MAX_RETRY="$2"
    shift
    ;;
  -d )
    CLI_DELAY="$2"
    shift
    ;;
  -s )
    DATABASE="$2"
    shift
    ;;
  -ccl )
    CC_SRC_LANGUAGE="$2"
    shift
    ;;
  -ccn )
    CC_NAME="$2"
    shift
    ;;
  -ccv )
    CC_VERSION="$2"
    shift
    ;;
  -ccs )
    CC_SEQUENCE="$2"
    shift
    ;;
  -ccp )
    CC_SRC_PATH="$2"
    shift
    ;;
  -ccep )
    CC_END_POLICY="$2"
    shift
    ;;
  -cccg )
    CC_COLL_CONFIG="$2"
    shift
    ;;
  -cci )
    CC_INIT_FCN="$2"
    shift
    ;;
  -i )
    IMAGETAG="$2"
    shift
    ;;
  -cai )
    CA_IMAGETAG="$2"
    shift
    ;;
  -verbose )
    VERBOSE=true
    shift
    ;;
  * )
    errorln "Unknown flag: $key"
    printHelpOrg3
    exit 1
    ;;
  esac
  shift
done

# Are we generating crypto material with this command?
if [ ! -d "organizations/peerOrganizations" ]; then
  CRYPTO_MODE="with crypto from '${CRYPTO}'"
else
  CRYPTO_MODE=""
fi

# Determine mode of operation and printing out what we asked for
if [ "$MODE" == "up" ]; then
  infoln "Starting nodes with CLI timeout of '${MAX_RETRY}' tries and CLI delay of '${CLI_DELAY}' seconds and using database '${DATABASE}' ${CRYPTO_MODE}"
elif [ "$MODE" == "createChannel" ]; then
  infoln "Creating channel '${CHANNEL_NAME}'."
  infoln "If network is not up, starting nodes with CLI timeout of '${MAX_RETRY}' tries and CLI delay of '${CLI_DELAY}' seconds and using database '${DATABASE} ${CRYPTO_MODE}"
elif [ "$MODE" == "down" ]; then
  infoln "Stopping network"
elif [ "$MODE" == "restart" ]; then
  infoln "Restarting network"
elif [ "$MODE" == "deployCC" ]; then
  infoln "deploying chaincode on channel '${CHANNEL_NAME}'"
else
  printHelpOrg3
  exit 1
fi

if [ "${MODE}" == "deployCC" ]; then
  deployCC $CHANNEL_NAME $CC_NAME $CC_SRC_PATH $CC_SRC_LANGUAGE $CC_VERSION $CC_SEQUENCE $CC_INIT_FCN $CC_END_POLICY $CC_COLL_CONFIG $CLI_DELAY $MAX_RETRY $VERBOSE

else
  printHelpOrg3
  exit 1
fi

