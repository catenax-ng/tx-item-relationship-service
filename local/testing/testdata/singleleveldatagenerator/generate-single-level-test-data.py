# Copyright (c) 2022,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
# Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import uuid
from json import dump
from os import listdir
from os import mkdir
from os.path import exists
from shutil import rmtree
from typing import Any

# collection of dictionaries, every dictionary must have keys:
# main_item_template, relationship_item_template, main_item_id_key_name,
# relationships_array_key_name, relationship_item_id_key_name, id_prefix
SINGLE_LEVEL_SEMANTIC_MODELS_TEMPLATES_SPECIFICATIONS = {
    "bom-as-built-3.0.0": {
        "main_item_template": {
            "catenaXId": "",
            "childItems": []
        }, "relationship_item_template": {
            "catenaXId": "",
            "quantity": {
                "value": 0.2014,
                "unit": "unit:kilogram"
            },
            "hasAlternatives": True,
            "businessPartner": "BPNL00000000TEST",
            "createdOn": "2022-02-03T14:48:54.709Z",
            "lastModifiedOn": "2022-02-03T14:48:54.709Z"
        }, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "childItems",
        "relationship_item_id_key_name": "catenaXId", "id_prefix": "urn:uuid:"
    },
    "bom-as-planned-2.0.0": {
        "main_item_template": {
            "catenaXId": "",
            "childItems": []
        }, "relationship_item_template": {
            "validityPeriod": {
                "validFrom": "2023-03-21T08:17:29.187+01:00",
                "validTo": "2024-07-01T16:10:00.000+01:00"
            },
            "catenaXId": "",
            "quantity": {
                "quantityNumber": 1,
                "measurementUnit": "unit:litre"
            },
            "businessPartner": "BPNL00000000TEST",
            "createdOn": "2022-02-03T14:48:54.709Z",
            "lastModifiedOn": "2022-02-03T14:48:54.709Z"
        }, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "childItems",
        "relationship_item_id_key_name": "catenaXId", "id_prefix": "urn:uuid:"
    },
    "bom-as-planned-3.0.0": {
        "main_item_template": {
            "catenaXId": "",
            "childItems": []
        }, "relationship_item_template": {
            "validityPeriod": {
                "validFrom": "2023-03-21T08:17:29.187+01:00",
                "validTo": "2024-07-01T16:10:00.000+01:00"
            },
            "catenaXId": "",
            "quantity": {
                "unit": "unit:piece",
                "value": 1
            },
            "businessPartner": "BPNL00000000TEST",
            "createdOn": "2022-02-03T14:48:54.709Z",
            "lastModifiedOn": "2022-02-03T14:48:54.709Z"
        }, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "childItems",
        "relationship_item_id_key_name": "catenaXId", "id_prefix": "urn:uuid:"
    },
    "bom-as-specified-2.0.0": {
        "main_item_template": {
            "assetId": "",
            "manufacturerId": "BPNL00000000TEST",
            "childItems": []
        }, "relationship_item_template": {
            "item": [{
                "itemClassification": [{
                    "value": "Door Key",
                    "key": "BPNL00000003CSGV:PartFamily"
                }],
                "itemQuantity": {
                    "quantityNumber": 20,
                    "measurementUnit": "unit:piece"
                },
                "ownerItemId": "urn:uuid:c4ae951f-b68d-462b-a58c-c029cc926630",
                "itemVersion": "05",
                "itemDescription": "The steering wheel is nice and round",
                "createdOn": "2022-02-03T14:48:54.709Z",
                "itemPositioning": "right",
                "lastModifiedOn": "2022-02-03T14:48:54.709Z"
            }],
            "childAssetId": "",
            "childItemCategory": "e.g. vehicle, winter wheels, bicycle rack"
        }, "main_item_id_key_name": "assetId", "relationships_array_key_name": "childItems",
        "relationship_item_id_key_name": "childAssetId", "id_prefix": "urn:uuid:"
    },
    "usage-as-built-3.0.0": {
        "main_item_template": {
            "catenaXId": "",
            "parentItems": [],
            "customers": ["BPNL00000000TEST"]
        }, "relationship_item_template": {
            "catenaXId": "",
            "isOnlyPotentialParent": False,
            "quantity": {
                "unit": "unit:piece",
                "value": 20
            },
            "businessPartner": "BPNL00000000TEST",
            "createdOn": "2022-02-03T14:48:54.709Z",
            "lastModifiedOn": "2022-02-03T14:48:54.709Z"
        }, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "parentItems",
        "relationship_item_id_key_name": "catenaXId", "id_prefix": "urn:uuid:"
    },
    "usage-as-planned-1.1.0": {
        "main_item_template": {
            "parentParts": [],
            "catenaXId": ""
        }, "relationship_item_template": {
            "validityPeriod": {
                "validFrom": "2023-03-21T08:47:14.438+01:00",
                "validTo": "2024-08-02T09:00:00.000+01:00"
            },
            "parentCatenaXId": "",
            "quantity": {
                "quantityNumber": 2.5,
                "measurementUnit": "unit:litre"
            },
            "createdOn": "2022-02-03T14:48:54.709Z",
            "lastModifiedOn": "2022-02-03T14:48:54.709Z"
        }, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "parentParts",
        "relationship_item_id_key_name": "parentCatenaXId", "id_prefix": "urn:uuid:"
    }
}


class FileSuffixCounterDict:
    """This class provides the possibility of keeping track of different counters
    by means of a dictionary. The counters indicate how many files have been generated
    for a level of recursion."""

    def __init__(self):
        self._file_suffix_counter_dict = {0: 0}

    def get_suffix_counter(self, recursion_level: int) -> int:
        try:
            return self._file_suffix_counter_dict[recursion_level]
        except KeyError:
            print(
                "Encountered logical error while retrieving file suffix counter, no entry in suffix counter map for "
                f"level {recursion_level}")
            exit(1)

    def initialize_new_suffix_counter(self, recursion_level: int) -> None:
        if recursion_level not in self._file_suffix_counter_dict.keys():
            self._file_suffix_counter_dict[recursion_level] = 0
        else:
            self.increment_suffix_counter(recursion_level)

    def increment_suffix_counter(self, recursion_level: int) -> None:
        try:
            self._file_suffix_counter_dict[recursion_level] += 1
        except KeyError:
            print(
                "Encountered logical error while incrementing file suffix counter, no entry in suffix counter map "
                f"for level {recursion_level}")
            exit(1)


class SemanticModelTemplate:
    """This class represents a template for a singleLevel semantic model.
    It constructs its fields from a dictionary which is passed to it
    (SINGLE_LEVEL_SEMANTIC_MODELS_INFO in this script). Its main purpose
    is to provide methods which leverage a given template to either generate a new
    singleLevel item or to get the template's properties.

    Arguments:
    template_name -- effectively one of the keys of SINGLE_LEVEL_SEMANTIC_MODELS_INFO\n
    model_info -- the value for template_name in SINGLE_LEVEL_SEMANTIC_MODELS_INFO
    """

    def __init__(self, template_name: str, model_info: dict[str, Any]):
        self._template_name = template_name
        self._main_item_template = model_info["main_item_template"]
        self._relationship_item_template = model_info["relationship_item_template"]
        self._main_item_id_key_name = model_info["main_item_id_key_name"]
        self._relationships_array_key_name = model_info["relationships_array_key_name"]
        self._relationship_item_id_key_name = model_info["relationship_item_id_key_name"]
        self._id_prefix = model_info["id_prefix"]

    def generate_main_item(self, num_relationship_items: int, uuid: str) -> dict[str, Any]:
        """Generate a semantic model's main item. The main item has almost always a list of relationships,
        depending on the semantic model.

        Arguments:
        num_relationship_items -- if the main item is supposed to have an array of relationship items
        populated, specify the number of relationships to generate\n
        uuid - the uuid for the item
        """

        new_item = self._main_item_template.copy()
        new_item[self._main_item_id_key_name] = uuid
        if num_relationship_items > 0:
            new_item[self._relationships_array_key_name] = [self.generate_relationship_item()
                                                            for _ in range(num_relationship_items)]

        return new_item

    def generate_relationship_item(self) -> dict[str, Any]:
        new_item = self._relationship_item_template.copy()
        new_item[self._relationship_item_id_key_name] = generate_catena_x_id(
            self._id_prefix)

        return new_item

    def get_template_name(self, make_camel_case: bool) -> str:
        # in SINGLE_LEVEL_SEMANTIC_MODELS_INFO, key names are kebab case like
        # return camel case display name
        if make_camel_case:
            split_parts = self._template_name.split("-")
            for i, string in enumerate(split_parts):
                split_parts[i] = string.capitalize()

            # enhance readability, e.g. bom-as-built-3.0.0 becomes BomAsBuilt_3.0.0
            # instead of BomAsBuilt3.0.0
            return "".join(split_parts[:-1]) + "-" + split_parts[-1]
        else:
            return self._template_name

    def get_relationship_item_id_key_name(self) -> str:
        return self._relationship_item_id_key_name

    def get_relationships_array_key_name(self) -> str:
        return self._relationships_array_key_name

    def get_id_prefix(self) -> str:
        return self._id_prefix


# generate a unique catenaXId
def generate_catena_x_id(prefix: str) -> str:
    return prefix + str(uuid.uuid4())


def generate_batch(uuid: str) -> dict[str, Any]:
    batch_3_0_0_info = {
        "main_item_template": {
            "localIdentifiers": [
                {
                    "value": "BPNL00000000TEST",
                    "key": "manufacturerId"
                },
                {
                    "value": "BID12345678",
                    "key": "batchId"
                }
            ],
            "manufacturingInformation": {
                "date": "2022-02-04T14:48:54Z",
                "country": "HUR"
            },
            "catenaXId": "",
            "partTypeInformation": {
                "manufacturerPartId": "123-0.740-3434-A",
                "classification": "product",
                "nameAtManufacturer": "Unspecified component"
            }
        }, "relationship_item_template": {}, "main_item_id_key_name": "catenaXId", "relationships_array_key_name": "",
        "relationship_item_id_key_name": "", "id_prefix": "urn:uuid:"
    }

    # parameters passed to generate_main_item are not used but for the uuid
    return SemanticModelTemplate("batch-3.0.0", batch_3_0_0_info).generate_main_item(0, uuid)


def generate_test_code_file(declarations: list[str], method_calls: list[str], depth: int,
                            test_code_file_name: str) -> None:
    """Write a list of Java variable declarations and a list of Java method calls to file with specified name, along
    with some supplementary code needed for integration testing."""

    first_headline = "#" * 3 + (" Insert below code into "
                                "IrsWireMockIntegrationTest.prepareBigTestDataSetAndReturnFirstGlobalAssetId()\n")

    second_headline = "#" * 3 + \
                      " If needed, insert below code into any test methods that use the data set\n"

    job_request_call = f"request = WiremockSupport.jobRequest(globalAssetId10, TEST_BPN, {depth});"

    file_content = (first_headline + "\n".join(declarations) + "\n\n" + "\n".join(method_calls) +
                    "\n\nreturn globalAssetId10;\n\n" + second_headline + job_request_call)

    with open(test_code_file_name, "w") as f:
        f.write(file_content)


def generate_test_data_set(template: SemanticModelTemplate, num_relationship_items: int, recursion_depth: int,
                           max_recursion_depth: int, uuid: str, file_suffix_counter_dict: FileSuffixCounterDict,
                           declarations: list[str], method_calls: list[str],
                           test_data_directory_name: str) -> tuple[list[str], list[str]]:
    current_suffix_counter = file_suffix_counter_dict.get_suffix_counter(
        recursion_depth)

    # recursion depth + 1 is performed purely for semantic purposes here - items should start at 1 instead of 0
    batch_file_name = f"{test_data_directory_name}/batch_{recursion_depth + 1}_{current_suffix_counter}.json"
    bom_file_name = (f"{test_data_directory_name}/singleLevel{template.get_template_name(make_camel_case=True)}"
                     f"_{recursion_depth + 1}_{current_suffix_counter}.json")

    stop_recursion = recursion_depth > max_recursion_depth
    num_relationship_items = num_relationship_items if not stop_recursion else 0

    batch = generate_batch(uuid)
    main_item = template.generate_main_item(num_relationship_items, uuid)

    # write batch json file
    with open(batch_file_name, 'w') as f:
        dump(batch, f, indent=2)

    # write singleLevelBomAsBuilt json file
    with open(bom_file_name, 'w') as f:
        dump(main_item, f, indent=2)

    # add current var declarations and method calls to lists
    if not stop_recursion:
        relationship_items = main_item[template.get_relationships_array_key_name(
        )]
        update_declarations_and_calls_lists_and_recurse(template, num_relationship_items, recursion_depth,
                                                        max_recursion_depth, uuid, file_suffix_counter_dict,
                                                        declarations, method_calls, test_data_directory_name,
                                                        batch_file_name, bom_file_name, batch, relationship_items)

    return declarations, method_calls


def update_declarations_and_calls_lists_and_recurse(template: SemanticModelTemplate, num_relationship_items: int,
                                                    recursion_depth: int, max_recursion_depth: int, uuid: str,
                                                    file_suffix_counter_dict: FileSuffixCounterDict,
                                                    declarations: list[str], method_calls: list[str],
                                                    test_data_directory_name: str, batch_file_name: str,
                                                    bom_file_name: str, batch: dict[str, Any],
                                                    relationship_items: list[dict[str, Any]]):
    current_suffix_counter = file_suffix_counter_dict.get_suffix_counter(
        recursion_depth)

    declarations.append(
        f"final String globalAssetId{recursion_depth + 1}{current_suffix_counter} = \"{uuid}\";")
    method_calls.append(
        f"successfulRegistryAndDataRequest(globalAssetId{recursion_depth + 1}{current_suffix_counter}, "
        f'\"{batch["partTypeInformation"]["nameAtManufacturer"]}\", TEST_BPN,\n'
        f"\"integrationtesting/{batch_file_name}\","
        f"\"integrationtesting/{bom_file_name}\");"
    )

    file_suffix_counter_dict.initialize_new_suffix_counter(recursion_depth + 1)

    for item in relationship_items:
        generate_test_data_set(template, num_relationship_items, recursion_depth + 1, max_recursion_depth,
                               item[template.get_relationship_item_id_key_name(
                               )], file_suffix_counter_dict, declarations,
                               method_calls, test_data_directory_name)
        file_suffix_counter_dict.increment_suffix_counter(recursion_depth + 1)


def determine_action_for_directory(model_name: str, directory_name: str, replace_if_necessary: bool) -> str:
    if directory_name == "":
        directory_name = model_name + "-test-data"

    if not exists(directory_name):
        print(
            f"Creating new target directory with name \"{directory_name}\"")
        mkdir(directory_name)
    else:
        if len(listdir(directory_name)) > 0:
            print(f"\nTarget directory \"{directory_name}\" already present in working directory and not empty.",
                  end=" ")

            if not replace_if_necessary:
                directory_name += "-" + str(uuid.uuid4())[:8]
                print(
                    f"\nCreating new target directory with name \"{directory_name}\".")

            else:
                print(f"Replacing it.")
                rmtree(directory_name)

            mkdir(directory_name)

    return directory_name


def cmdline_args_test_data_generation(semantic_model_name: str, num_relationship_items: int, max_recursion_depth: int,
                                      target_directory: str, replace_target_if_necessary: bool) -> None:
    template = SemanticModelTemplate(semantic_model_name,
                                     SINGLE_LEVEL_SEMANTIC_MODELS_TEMPLATES_SPECIFICATIONS[semantic_model_name])

    full_model_name = f"singleLevel{template.get_template_name(make_camel_case=True)}"
    directory_name = determine_action_for_directory(
        full_model_name, target_directory, replace_target_if_necessary)

    variable_declarations, method_calls = generate_test_data_set(
        template=template,
        num_relationship_items=num_relationship_items,
        recursion_depth=0,
        max_recursion_depth=max_recursion_depth,
        uuid=generate_catena_x_id(template.get_id_prefix()),
        file_suffix_counter_dict=FileSuffixCounterDict(),
        declarations=[],
        method_calls=[],
        test_data_directory_name=directory_name)

    generate_test_code_file(variable_declarations, method_calls,
                            max_recursion_depth, f"{directory_name}/test_code.txt")

    print(
        f"\nSuccessfully saved generated test data set in directory \"{directory_name}\".")


def integer_greater_than_zero(integer: str) -> int:
    """This is meant to solely be a type which can be specified in the below calls to argparse."""
    num = int(integer)
    if num < 1:
        raise ValueError()
    return num


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="This Python script generates a hierarchy of singleLevel semantic model files along with "
                    "associated batch files for testing purposes. "
                    "It uses Python dictionaries included in the code as templates for the json objects.",
        usage="%(prog)s [options]"
    )
    parser.add_argument("--model", choices=SINGLE_LEVEL_SEMANTIC_MODELS_TEMPLATES_SPECIFICATIONS.keys(), nargs=1,
                        required=True, help="Specify the singleLevel semantic model for which you want to generate"
                                            "the test data set.")
    parser.add_argument("--relationships", type=integer_greater_than_zero, nargs=1, required=True,
                        help="Specify how many relationships each item should have. "
                             "Once the recursion level specified by --depth has been exceeded, the "
                             "models will not have any relationships and thus the recursion will come "
                             "to a halt.")
    parser.add_argument("--depth", type=integer_greater_than_zero, nargs=1, required=True,
                        help="Specify recursion depth that the test data should satisfy.")
    parser.add_argument("--target", nargs=1, required=False, default=[""],
                        help="(Optional) Name of directory in which to place generated test data files (default: "
                             "chosen model's name + \"-generated-test-data\")")
    parser.add_argument("--replace", action="store_true",
                        help="(Optional) If the value of --target points to a directory which is already present and"
                             "not empty and this switch is set, delete that directory, create a new one with the same "
                             "name, and populate it with the generated test data set. Else, by default, a random "
                             "directory name is generated.")

    args = parser.parse_args()

    cmdline_args_test_data_generation(
        args.model[0], args.relationships[0], args.depth[0], args.target[0], args.replace)
