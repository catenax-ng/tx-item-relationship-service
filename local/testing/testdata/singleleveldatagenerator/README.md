[[Back to main IRS README](../../../../README.md)]

# generate-single-level-test-data.py

## Prerequisites

Python 3.10 or higher has to be installed.

## Usage

```shell
python generate-single-level-test-data.py [ options ]
```

## Description

This script generates a hierarchy of singleLevel semantic model items, or rather files. Currently, it also generates
a batch file for each item using the Batch 3.0.0 model, which is probably going to change in the future. Additionally,
it generates a text file which contains Java code that can be pasted into
[IrsWireMockIntegrationTest](../../../../irs-api/src/test/java/org/eclipse/tractusx/irs/IrsWireMockIntegrationTest.java).

### Semantic model template specifications

The code contains a dictionary consisting of template specifications for the semantic models.
These specifications are dictionaries themselves and they all have the same keys:

- `main_item_template`: this is the template for the part of a semantic model which always carries at least the
  semantic model item's id and which typically also has an array of relationship items.
- `relationship_item_template`: this is a template for an element in the array of relationship items.
- `main_item_id_key_name`: this is used to identify the key in the dictionary whose value holds the id
  of the semantic model item. The value of that key is always an empty string.
- `relationships_array_key_name`: just the same as the main_item_id_key_name, this is used to identify
  the key whose value holds the array of relationship items. The value of that key is always an empty array.
- `relationship_item_id_key_name`: every element in the array of relationship items has an id, and this identifies
  the key that holds it. Like the id of the main item, this is an empty string.
- `id_prefix`: this is used to prefix any ids.

The purpose of this is reusability: if a specification for a model template gets added to the dictionary, but it
adheres to the structure, it is possible to generate a hierarchy of test data for the new model without
changing any code in the script other than the dictionary.

The below example for the `SingleLevelBomAsBuilt:3.0.0` model illustrates the structure:

```json
{
  "bom-as-built-3.0.0": {
    "main_item_template": {
      "catenaXId": "",
      "childItems": []
    },
    "relationship_item_template": {
      "catenaXId": "",
      "quantity": {
        "value": 0.2014,
        "unit": "unit:kilogram"
      },
      "hasAlternatives": True,
      "businessPartner": "BPNL00000000TEST",
      "createdOn": "2022-02-03T14:48:54.709Z",
      "lastModifiedOn": "2022-02-03T14:48:54.709Z"
    },
    "main_item_id_key_name": "catenaXId",
    "relationships_array_key_name": "childItems",
    "relationship_item_id_key_name": "catenaXId",
    "id_prefix": "urn:uuid:"
  }
}
```

### Semantic model selection

The script has several options, and among them is `--model`, which allows for the selection of the semantic model
for which the test data should be generated. The currently added models are:

- `SingleLevelBomAsBuilt:3.0.0` -> pass on the command line as `--model bom-as-built-3.0.0`
- `SingleLevelBomAsPlanned:2.0.0` -> pass on the command line as `--model bom-as-planned-2.0.0`
- `SingleLevelBomAsPlanned:3.0.0` -> pass on the command line as `--model bom-as-planned-3.0.0`
- `SingleLevelBomAsSpecified:2.0.0` -> pass on the command line as `--model bom-as-specified-2.0.0`
- `SingleLevelUsageAsBuilt:3.0.0` -> pass on the command line as `--model usage-as-built-3.0.0`
- `SingleLevelUsageAsPlanned:1.1.0` -> pass on the command line as `--model usage-as-planned-1.1.0`

In case you modify the script's dictionary of templates, please also update the above list. The dictionary
can be found at the very top of the script file with a variable name of
`SINGLE_LEVEL_SEMANTIC_MODELS_TEMPLATES_SPECIFICATIONS`.

### Test data set size selection

#### Number of relationships for every item

To determine how many relations every semantic model item should contain in its array at `relationships_array_key_name`,
you must specify a number greater than 0 for the option `--relationships`.

Every relationship array's size will be exactly the number you specify. This is true as long as the test data's
recursion depth has not been reached.

#### Test data recursion depth

To further determine the extent of the test data hierarchy, you must specify what recursion depth, or rather request
depth, the test data should satisfy. You do so using the `--depth` parameter with an integer greater than 0. The script
keeps track of how deep it is in the recursion and stops adding relationships to the array of relationship items once
the depth you specified has been exceeded.

### Test data set files location selection

You can optionally specify the name of the directory in which to put the generated files with `--target`. If you
do not specify a name, a default name will be built by concatenation of the selected semantic model's name and the
string `"-generated-test-data"`.

Whether you specify `--target` or not, if a directory with the effectively chosen name
is already present, a string of random hexadecimal numbers will be appended to the name, so that the directories
can coexist. You can override this default behavior by specifying the `--replace` option, which tells the script
to replace the already present contents with the newly generated ones.

### How the test data hierarchy generation works

Essentially, the script follows these steps:

1. For the effectively selected template specification, it copies the sub-dictionary at `main_item_template`.
2. It builds an id string by concatenation of `id_prefix` and a random UUIDv4 string.
3. In its current development state, the script copies a template for the `Batch:3.0.0` model which has a key
   that the script sets to the newly generated id. This is currently performed with every semantic model,
   irrespective of if it makes sense.
4. After that, it replaces the copied sub-dictionary's empty string at `main_item_id_key_name`
   with the newly generated id.
5. If the recursion depth has not reached the limit specified using `--depth` yet, the script populates the
   empty array at `relationships_array_key_name` with exactly the number of relationship items
   specified using `--relationships`. To generate each relationship item, it copies `relationship_item_template`
   and sets a generated id as the value of the key identified by the value of `relationship_item_id_key_name`.
6. Both the batch and the main item with the empty / populated array of relationship items are written to .json
   files in the selected location.
7. If the recursion depth limit has not been reached yet, repeat the preceding steps for every relationship item
   in the array.
8. Once recursion is done, write a text file with Java code useful for testing using data collected along
   the flow of execution. The file is written to the selected location.

### Text file with Java code useful for testing

The script writes a text file which contains Java code useful for testing. The text file's contents look like
this:
```text
### Insert below code into IrsWireMockIntegrationTest.prepareBigTestDataSetAndReturnFirstGlobalAssetId()
final String globalAssetId10 = "urn:uuid:356ba3e3-6e75-46cd-9fac-ba00ede2c708";
final String globalAssetId20 = "urn:uuid:79a91e83-f5b3-41de-b8c1-2446ce7cf584";
final String globalAssetId21 = "urn:uuid:645b19ef-1796-48b7-a622-4ea5771e05b0";

successfulRegistryAndDataRequest(globalAssetId10, "Unspecified component", TEST_BPN,
"integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/batch_1_0.json","integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/singleLevelBomAsBuilt-3.0.0_1_0.json");
successfulRegistryAndDataRequest(globalAssetId20, "Unspecified component", TEST_BPN,
"integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/batch_2_0.json","integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/singleLevelBomAsBuilt-3.0.0_2_0.json");
successfulRegistryAndDataRequest(globalAssetId21, "Unspecified component", TEST_BPN,
"integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/batch_2_1.json","integrationtesting/local/testing/testdata/singleleveldatagenerator/generated-test-data/singleLevelBomAsBuilt-3.0.0_2_1.json");

return globalAssetId10;

### If needed, insert below code into any test methods that use the data set
request = WiremockSupport.jobRequest(globalAssetId10, TEST_BPN, 1);
```

## Parameters

If not mentioned otherwise, all parameters are mandatory for the script to work.

**--model**  
Specify the singleLevel semantic model for which you want to generate the test data set.
You can find the currently available models in this document, [here](#semantic-model-selection).

**--relationships**
Specify how many relationships each item should have. This must be an integer > 0. Once the recursion level specified by
--depth has been exceeded, the models will not have any relationships and the recursion will come to a halt.

**--depth**
Specify the recursion depth that the test data should satisfy. This must be an integer > 0.

**--target**
(Optional) Name of directory in which to place generated test data files
(default: `chosen model's name in Camel Case + "-generated-test-data"`).

**--replace**
(Optional) If the value of --target points to a directory which is already present and not empty and this switch is set,
delete that directory, create a new one with the same name, and populate it with the generated test data set.
(default: `value of --target + '-' + eight random hexadecimal numbers `.

**-h, --help**  
Usage help. This lists all current command line options with a short description.
